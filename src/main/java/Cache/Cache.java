package Cache;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;

public class Cache<Key, Value> {
    private final PersistenceAlgorithm persistenceAlgorithm;
    private final EvictionAlgorithm evictionAlgorithm;
    private final DataSource<Key, Value> dataSource;
    private final Map<Key, CompletableFuture<Record<Value>>> cacheMap;
    private final ConcurrentSkipListMap<Long, List<Key>> expiryQueue;
    private final ConcurrentSkipListMap<AccessDetails, List<Key>> priorityQueue;
    private final ArrayList<ExecutorService> threadPool;
    private final List<Key> hotLoad;
    private final long EXPIRE_DURATION;
    private final Integer POOL_SIZE;
    private static final Integer THRESHOLD_SIZE = 500;

    public Cache(PersistenceAlgorithm persistenceAlgorithm, EvictionAlgorithm evictionAlgorithm, DataSource<Key, Value> dataSource, int expiryTimeInMillis, Map<Long, List<Record<Value>>> expiryQueue, ExecutorService executor, List<Key> hotLoad, long expireDuration, Integer poolSize) throws ExecutionException, InterruptedException {
        this.persistenceAlgorithm = persistenceAlgorithm;
        this.evictionAlgorithm = evictionAlgorithm;
        this.dataSource = dataSource;
        this.hotLoad = hotLoad;
        EXPIRE_DURATION = expireDuration;
        POOL_SIZE = poolSize;
        cacheMap = new ConcurrentHashMap<>();
        this.expiryQueue = new ConcurrentSkipListMap<>();
        this.priorityQueue = new ConcurrentSkipListMap<>(compareAccessDetails);
        threadPool = new ArrayList<>(POOL_SIZE);
        for(int i = 0; i < POOL_SIZE; i++){
            threadPool.set(i,Executors.newSingleThreadExecutor());
        }
        for(Key key : hotLoad){
            dataSource.get(key).thenAccept(value -> {
                try {
                    addToCache(key, value);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public Comparator<AccessDetails> compareAccessDetails = new Comparator<AccessDetails>() {
        @Override
        public int compare(AccessDetails first, AccessDetails second) {
            if(evictionAlgorithm == EvictionAlgorithm.LFU && first.getAccessCount() != second.getAccessCount()) {
                return (int) (first.getAccessCount() - second.getAccessCount());
            } else {
                return (int) (first.getAccessTime() - second.getAccessTime());
            }
        }
    };

    public CompletableFuture<Value> get(Key key) {
        return CompletableFuture
                .supplyAsync(() -> getInAssignedThread(key), threadPool.get(key.hashCode()%POOL_SIZE))
                .thenCompose(Function.identity());
    }

    public CompletableFuture<Value> getInAssignedThread(Key key) {
        final CompletableFuture<Record<Value>> result;
        if(cacheMap.containsKey(key)) {
            result = cacheMap.get(key);
        } else {
            resizeCache();
            result = dataSource.get(key)
                    .thenCompose(value -> {
                        try {
                            return addToCache(key, value);
                        } catch (ExecutionException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
        return result.thenApply(record -> {
            AccessDetails accessDetails = record.getAccessDetails();
            if(priorityQueue.get(accessDetails)!=null){
                List<Key> keys = priorityQueue.get(accessDetails);
                if(keys.size() == 1){
                    priorityQueue.remove(accessDetails);
                } else {
                    keys.remove(key);
                }
            }
            accessDetails.setAccessCount(accessDetails.getAccessCount()+1);
            accessDetails.setAccessTime(System.currentTimeMillis());
            priorityQueue.putIfAbsent(accessDetails, new CopyOnWriteArrayList<>());
            priorityQueue.get(accessDetails).add(key);
            return record.getValue();
        });
    }

    public Future<Void> set(Key key, Value value){
        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return setInAssignedThread(key, value);
                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                },threadPool.get(key.hashCode()%POOL_SIZE))
                .thenCompose(Function.identity());
    }

    private void resizeCache(){
        if(cacheMap.size() > THRESHOLD_SIZE) {
            Key firstKey = priorityQueue.pollFirstEntry().getValue().getFirst();
            cacheMap.remove(firstKey);
            expiryQueue.remove(firstKey);
            expireData();
        }
    }

    private CompletableFuture<Void> setInAssignedThread(Key key, Value value) throws ExecutionException, InterruptedException {
        if(cacheMap.containsKey(key)) {
            CompletableFuture<Record<Value>> temp = cacheMap.remove(key);
            priorityQueue.remove(temp.get().getAccessDetails());
            expiryQueue.remove(temp.get().getLoadTime());
        }
        resizeCache();
        if(persistenceAlgorithm.equals(PersistenceAlgorithm.WRITE_THROUGH)) {
            dataSource.persist(key, value).thenAccept(__ -> {
                try {
                    addToCache(key, value);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            addToCache(key, value);
            dataSource.persist(key, value);
        }
        return CompletableFuture.completedFuture(null);
    }

    private void expireData() {
        long now = System.currentTimeMillis();
        while(expiryQueue.firstKey()<=now-EXPIRE_DURATION) {
            Key firstKey = expiryQueue.pollFirstEntry().getValue().getFirst();
            cacheMap.remove(firstKey);
            priorityQueue.remove(firstKey);
        }
    }

    private CompletableFuture<Record<Value>> addToCache(Key key, Value value) throws ExecutionException, InterruptedException {
        CompletableFuture<Record<Value>> record = CompletableFuture.completedFuture(new Record<Value>(value));
        cacheMap.put(key, record);
        expiryQueue.putIfAbsent(record.get().getLoadTime(), new CopyOnWriteArrayList<>());
        expiryQueue.get(record.get().getLoadTime()).add(key);
        priorityQueue.putIfAbsent(record.get().getAccessDetails(), new CopyOnWriteArrayList<>());
        priorityQueue.get(record.get().getAccessDetails()).add(key);
        return cacheMap.get(key);
    }
}


