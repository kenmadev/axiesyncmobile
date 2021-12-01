import BackgroundService from 'react-native-background-actions';
import {EventEmitter} from 'events';
import isEmpty from 'lodash/isEmpty';
import isEqual from 'lodash/isEqual';
import {
  readFile,
  openDocumentTree,
  getPersistedUriPermissions,
  releasePersistableUriPermission,
  getDocumentUriFromTree,
  statFile,
} from './module';
import {useAxios} from './utils';
import {AXIEPACKAGENAME, REMOTEAPI} from './config';

let prevStats = {};
const initialPath = `Android/data`;
const battleHistoryFile = 'production-battleHistoriesState';
const events = new EventEmitter();

const syncBattle = async data => {
  try {
    // read the first item on the list and sync it
    const {battles} = data;
    const [battle = {}] = battles;

    if (isEmpty(battle)) {
      throw new Error('Battle data is empty');
    }

    // sync to server
    await BackgroundService.updateNotification({
      taskDesc: 'Recent battle is being sync',
    });
    await useAxios().post(`${REMOTEAPI}/battles`, {...battle});
    await BackgroundService.updateNotification({
      taskDesc: 'Recent battle successfully synced',
    });
  } catch (err) {
    console.log(`Unable to sync battle history with error`, err.message);
    await BackgroundService.updateNotification({
      taskDesc: 'Recent battle failed to sync',
    });
  } finally {
    await new Promise(resolve => setTimeout(resolve, 5000));
    await BackgroundService.updateNotification({
      taskDesc: 'Battle history is being monitored',
    });
  }
};

const fileWatcher = async (documentUri, interval = 5000) => {
  while (true) {
    try {
      if (!BackgroundService.isRunning()) {
        events.emit('stop');
        events.removeAllListeners();
        break;
      }

      const stats = await statFile(documentUri);
      if (stats.type === 'file') {
        if (isEmpty(prevStats)) {
          prevStats = Object.assign({}, prevStats, stats);
        }

        const {lastModified} = prevStats;
        const prevMtimeUnix = lastModified;
        const newMtimeUnix = stats.lastModified;
        if (newMtimeUnix > prevMtimeUnix) {
          prevStats = Object.assign({}, prevStats, stats);
          // read the file
          const file = await readFile(documentUri, 'utf8');
          events.emit('change', JSON.parse(file));
        }
      }
    } catch (err) {
      events.emit('error', err);
    } finally {
      await new Promise(resolve => setTimeout(resolve, interval));
    }
  }
};

const task = (documentUri, stats) => () => {
  return new Promise(async (_resolve, reject) => {
    try {
      // watch the file for changes
      events
        .on('change', async data => {
          console.log(`Watcher file has changed`);
          syncBattle(data);
        })
        .on('ready', stats => {
          console.log('Watcher for battle cache is ready!', stats);
        })
        .on('stop', () => {
          console.log('Watcher has been stopped.');
        })
        .on('error', err => {
          console.log('Watcher error', err.message);
        });

      fileWatcher(documentUri);
      events.emit('ready', stats);
    } catch (err) {
      console.log('Background task failed with error', err.message);
      reject(err);
    }
  });
};

const permissionHasPersistTree = permissions =>
  !isEmpty(permissions.find(perm => perm.includes(AXIEPACKAGENAME)));

const getTreeUri = () => {
  return new Promise(async (resolve, reject) => {
    try {
      const permissions = await getPersistedUriPermissions();
      console.log('Permissions:', permissions);
      if (isEmpty(permissions)) {
        const result = await openDocumentTree(initialPath, true);
        if (!('uri' in result)) throw new Error('Tree uri not found');
        return resolve(result.uri);
      }

      // get from persisted perms
      const uri = permissions.find(perm => perm.includes(AXIEPACKAGENAME));
      resolve(uri);
    } catch (err) {
      await purgePersistedPermissions();
      reject(
        new Error(
          `AXIE_URI_ERROR: Invalid axie folder, make sure you selected the right folder. \`${initialPath}/${AXIEPACKAGENAME}\``,
        ),
      );
    }
  });
};

const purgePersistedPermissions = () => {
  return new Promise(async (resolve, reject) => {
    try {
      const permissions = await getPersistedUriPermissions();
      for (const uri of permissions) {
        await releasePersistableUriPermission(uri);
      }

      resolve();
    } catch (err) {
      reject(err);
    }
  });
};

const watch = () => {
  return new Promise(async (resolve, reject) => {
    try {
      // return purgePersistedPermissions();

      // build documentId, ask permissions if possible
      const treeUri = await getTreeUri();
      if (!treeUri.includes(AXIEPACKAGENAME)) {
        await purgePersistedPermissions();
        throw new Error(
          `INVALID_DATA_FOLDER: Invalid axie folder, make sure you selected the right folder. \`${initialPath}/${AXIEPACKAGENAME}\``,
        );
      }

      const documentId = `primary:${initialPath}/${AXIEPACKAGENAME}/files/${battleHistoryFile}`;
      const documentUri = await getDocumentUriFromTree(treeUri, documentId);
      console.log('documentUri', documentUri);

      if (isEmpty(documentUri)) {
        throw new Error('NO_FILE_URI: No valid uri has been generated.');
      }

      // initial check
      const stats = await new Promise(async (resolve, reject) => {
        try {
          const stats = await statFile(documentUri);
          const isValid = [
            isEqual(stats.type, 'file'),
            stats.lastModified >= 0,
          ].every(value => !!value);

          if (!isValid) {
            throw new Error(
              `Unable to identify the given battle cache file. Make sure the game is installed or atleast one battle history has been saved.`,
            );
          }

          resolve(stats);
        } catch (err) {
          reject(new Error(`INVALID_CACHE_FILE: ${err.message}`));
        }
      });

      // start background task
      const options = {
        taskName: 'axiesyncmobiletask',
        taskTitle: 'Axie Sync',
        taskDesc: 'Battle history is being monitored',
        taskIcon: {
          name: 'ic_launcher',
          type: 'mipmap',
        },
        linkingURI: 'axiesync://view',
        color: '#1976d2',
      };

      await BackgroundService.start(task(documentUri, stats), options);
      resolve();
    } catch (err) {
      reject(err);
    }
  });
};

const unwatch = () => {
  return new Promise(async (resolve, reject) => {
    try {
      await BackgroundService.stop();
      resolve();
    } catch (err) {
      reject(err);
    }
  });
};

module.exports = {
  watch,
  unwatch,
  permissionHasPersistTree,
  purgePersistedPermissions,
  getPersistedUriPermissions,
};
