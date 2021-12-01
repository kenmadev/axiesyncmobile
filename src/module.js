import {NativeModules} from 'react-native';
const {AxieSyncModule} = NativeModules;

export const requestPermission = () => AxieSyncModule.requestPermission();
export const hasPermission = () => AxieSyncModule.hasPermission();
export const statFile = documentUri => AxieSyncModule.stat(documentUri);
export const openDocument = (readdata = true, enconding = 'utf8') =>
  AxieSyncModule.openDocument(readdata, enconding);

export const readFile = (filepath, encoding = 'utf8') =>
  AxieSyncModule.readFile(filepath, encoding);

export const getPersistedUriPermissions = () =>
  AxieSyncModule.getPersistedUriPermissions();

export const releasePersistableUriPermission = uri =>
  AxieSyncModule.releasePersistableUriPermission(uri);

export const getDocumentUriFromTree = (uriTree, documentId) =>
  AxieSyncModule.getDocumentUriFromTree(uriTree, documentId);

export const openDocumentTree = (
  initialpath = 'Android/data',
  persist = true,
) => AxieSyncModule.openDocumentTree(initialpath, persist);
