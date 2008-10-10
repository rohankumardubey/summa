package dk.statsbiblioteket.summa.storage.api.rmi;

import dk.statsbiblioteket.summa.storage.api.ReadableStorage;
import dk.statsbiblioteket.summa.common.Record;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA. User: mikkel Date: Aug 5, 2008 Time: 1:02:08 PM To
 * change this template use File | Settings | File Templates.
 */
public interface RemoteReadableStorage extends ReadableStorage, Remote {

    Iterator<Record> getRecordsFromBase(String base) throws RemoteException;

    Iterator<Record> getRecordsModifiedAfter(long time, String base) throws RemoteException;

    Iterator<Record> getRecordsFrom(String id, String base) throws RemoteException;

    List<Record> getRecords(List<String> ids, int expansionDepth) throws RemoteException;

    Record getRecord(String id, int expansionDepth) throws RemoteException;

    Record next(Long iteratorKey) throws RemoteException;

    List<Record> next(Long iteratorKey, int maxRecords) throws RemoteException;

}



