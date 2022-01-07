## Usage
```java
public void oneMethod() {
    KeyLock<String> lock = KeyLock.getLock("the key");
    lock.lock(); // 支持Lock接口的所有方法
    try {
        // ...
    } finally {
        lock.unlock();
    }
}
```
