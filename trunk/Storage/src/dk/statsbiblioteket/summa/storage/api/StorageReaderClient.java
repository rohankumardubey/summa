package dk.statsbiblioteket.summa.storage.api;

import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.IOException;
import java.util.List;

/**
 * A helper class utilizing a stateless connection to a storage service exposing
 * a {@link ReadableStorage} interface. Unless your needs are very advanced
 * or you must do manual connection management, this is by far the
 * easiest way to use a remote {@link ReadableStorage}.
 * <p></p>
 * This class is modelled as a {@link ConnectionConsumer} meaning that you can
 * tweak its behavior by changing the configuration parameters
 * {@link dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory#CONF_RETRIES},
 * {@link dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory#CONF_GRACE_TIME},
 * {@link dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory#CONF_FACTORY}, and
 * {@link ConnectionConsumer#CONF_RPC_TARGET}.
 * <p/>
 * The property {@link ConnectionConsumer#CONF_RPC_TARGET} <i>must</i> be
 * defined while all others are optional.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke")
public class StorageReaderClient extends ConnectionConsumer<ReadableStorage>
                                 implements Configurable, ReadableStorage {

    public StorageReaderClient(Configuration conf) {
        super (conf);
    }

    @Override
    public long getRecordsModifiedAfter(long time, String base,
                                        QueryOptions options)
                                                            throws IOException {
        ReadableStorage storage = getConnection();

        try {
            return storage.getRecordsModifiedAfter(time, base, options);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("getRecordsModifiedAfter("+time+", "+base
                                  +") failed: " + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }

    @Override
    public long getModificationTime (String base) throws IOException {
        ReadableStorage storage = getConnection();

        try {
            return storage.getModificationTime (base);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("getModificationTIme("+base+") failed: "
                                  + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }

    @Override
    public List<Record> getRecords(List<String> ids, QueryOptions options)
                                                            throws IOException {
        ReadableStorage storage = getConnection();

        try {
            return storage.getRecords(ids, options);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("getRecords("+ Logs.expand(ids, 10)
                                  +", options="+ options + ") failed: "
                                  + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }

    @Override
    public Record getRecord(String id, QueryOptions options) throws IOException {
        ReadableStorage storage = getConnection();

        try {
            return storage.getRecord(id, options);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("getRecord("+id+", options="
                                  + options + ") failed: "
                                  + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }

    @Override
    public Record next(long iteratorKey) throws IOException {
        ReadableStorage storage = getConnection();

        try {
            return storage.next(iteratorKey);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("next("+iteratorKey+") failed: "
                                  + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }

    @Override
    public List<Record> next(long iteratorKey, int maxRecords)
                                                            throws IOException {
        ReadableStorage storage = getConnection();

        try {
            return storage.next(iteratorKey, maxRecords);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("next("+iteratorKey+", "+maxRecords
                                  +") failed: " + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }

}



