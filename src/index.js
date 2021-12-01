/**
 * @format
 * @flow strict-local
 */

import {isEmpty} from 'lodash';
import React, {useEffect, useState, useCallback} from 'react';
import {StatusBar} from 'react-native';
import styled from 'styled-components/native';
import {ConfirmProvider, useConfirm} from 'react-native-confirm-dialog';
import AxieSync from './sync';

const App = () => {
  const [synced, syncReady] = useState(false);
  const [error, setError] = useState('');
  const [persisted, setPersisted] = useState(false);
  const confirm = useConfirm();

  useEffect(() => {
    handleTreePersist();
  }, []);

  const onSync = useCallback(() => {
    console.log('persisted', persisted);
    if (persisted) {
      AxieSync.watch()
        .then(() => syncReady(true))
        .catch(err => setError(err.message));
    }
  });

  const onUnSync = useCallback(() => {
    AxieSync.unwatch()
      .then(() => syncReady(false))
      .catch(err => setError(err.message));
  });

  const onTryAgain = () => setError('');
  const handleTreePersist = () => {
    AxieSync.getPersistedUriPermissions()
      .then(permissions => {
        const hasPersistedTree = AxieSync.permissionHasPersistTree(permissions);
        setPersisted(hasPersistedTree);
      })
      .catch(err =>
        setError(
          `TREE_PERMISSION_ERROR: Unable to identify the persisted tree permissions. ${err.message}`,
        ),
      );
  };

  const onConfirmNotice = () => setPersisted(true);

  const renderContent = () => {
    if (!persisted) {
      return (
        <>
          <Division>
            <CenterText>Please read and understand carefully.</CenterText>
            <Divider />
            <CenterText>
              In order for the app to read your Axie Infinity battle history. It
              needs to know where is the location of the Axie Infinity data
              folder. The Axie Infinity data folder is usually sitting under{' '}
              <Highlight>root/Android/data/com.axieinfinity.origin</Highlight>{' '}
              folder. The <Highlight>root</Highlight> is the first or top-most
              directory in your internal storage.
            </CenterText>
            <Divider />
            <CenterText>
              The next screen will open your file explorer after you tap the{' '}
              <Highlight>Sync</Highlight> button and it will try its best to
              open the right folder for you. But in case it doesn't, please try
              to point it to the right one as mentioned above. If you're in the
              right folder, you will see another 2 folders. The{' '}
              <Highlight>cache</Highlight> and <Highlight>files</Highlight>{' '}
              folder.
            </CenterText>
          </Division>
          <ActionView>
            <ActionButton onPress={onConfirmNotice}>
              <ActionButtonText>Confirm</ActionButtonText>
            </ActionButton>
          </ActionView>
        </>
      );
    }

    if (!isEmpty(error)) {
      return (
        <>
          <Division>
            <CenterText>
              There was an error when trying to monitor the battle logs. Please
              restart the app and try again. If problem persist, please contact
              your manager.
            </CenterText>
            <Divider />
            <ErrorText>Error details: {error}</ErrorText>
          </Division>
          <ActionView>
            <ActionButton red={true} onPress={onTryAgain}>
              <ActionButtonText>Try Again</ActionButtonText>
            </ActionButton>
          </ActionView>
        </>
      );
    }

    if (!synced) {
      return (
        <>
          <Division>
            <CenterText>
              This application will sync your axie battle history to the cloud
              and help your managers view your battle replays easily. Tap the
              Sync button to start.
            </CenterText>
          </Division>
          <ActionView>
            <ActionButton onPress={onSync}>
              <ActionButtonText>Sync</ActionButtonText>
            </ActionButton>
          </ActionView>
        </>
      );
    }

    return (
      <>
        <Division>
          <CenterText>Axie battle history is now being monitored!</CenterText>
          <CenterText>Please minimize this app and get into battle.</CenterText>
        </Division>
        <ActionView>
          <ActionButton red={true} onPress={onUnSync}>
            <ActionButtonText>UnSync</ActionButtonText>
          </ActionButton>
        </ActionView>
      </>
    );
  };

  return (
    <SafeAreaView>
      <StatusBar
        translucent={true}
        backgroundColor="#282c34"
        barStyle="light-content"
      />
      <HomeView>
        <ConfirmProvider>{renderContent()}</ConfirmProvider>
      </HomeView>
    </SafeAreaView>
  );
};

const SafeAreaView = styled.SafeAreaView`
  flex: 1;
  background-color: #282c34;
`;

const HomeView = styled.View`
  flex: 1;
  justify-content: center;
  align-items: center;
  padding: 22px;
`;

const Division = styled.View`
  flex: 1;
  align-items: center;
  justify-content: center;
`;

const CenterText = styled.Text`
  text-align: center;
  color: #fff;
  font-size: 16px;
  line-height: 24px;
`;

const ActionView = styled.View`
  width: 100%;
`;

const Highlight = styled.Text`
  font-size: 15px;
  font-weight: bold;
  text-decoration: underline;
`;

const ActionButton = styled.TouchableOpacity`
  background-color: ${props => (props.red ? '#ef5350' : '#1976d2')}
  height: 50px;
`;

const Divider = styled.View`
  margin-bottom: 20px;
`;

const ActionButtonText = styled.Text`
  color: #fff;
  font-size: 14px;
  align-self: center;
  text-transform: uppercase;
  padding: 16px;
`;

const ErrorText = styled.Text`
  background-color: #afafaf;
  padding: 15px;
  margin-left: 10px;
  margin-right: 10px;
`;

export default App;
