package Cache;

import lombok.Getter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Getter
public class Record<Value> implements Comparable<Record<Value>> {
    private final Value value;
    private final long loadTime;
    private final AccessDetails accessDetails;

    public Record(Value value) {
        this.value = value;
        this.loadTime = System.currentTimeMillis();
        this.accessDetails = new AccessDetails(loadTime);
    }

    @Override
    public int compareTo(Record record) {
        return (int) (this.loadTime-record.loadTime);
    }
}
