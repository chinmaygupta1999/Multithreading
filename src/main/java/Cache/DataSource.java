package Cache;

import java.util.concurrent.CompletableFuture;

public class DataSource<Key, Value> {
    public CompletableFuture<Value> get(Key key){
        return null;
    }

    public CompletableFuture<Void> persist(Key key, Value value){
        return null;
    }
}

