package Cache;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.Future;

@Getter
@Setter
public class AccessDetails {
    private long accessTime;
    private long accessCount;

    public AccessDetails(long accessTime){
        this.accessTime = accessTime;
        this.accessCount = 0;
    }

    public Future<Void> setAccessTime(long accessTime){
        this.accessTime = accessTime;
        return null;
    }

    public Future<Void> setAccessCount(long accessCount){
        this.accessCount = accessCount;
        return null;
    }

}
