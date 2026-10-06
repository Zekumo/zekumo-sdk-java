# Zekumo Java SDK

Official Java 17+ asynchronous SDK. Uses `java.net.http` for REST/WebSocket and
Jackson for JSON, covering every player endpoint plus rooms and chat.

```java
var zk = new ZekumoClient("zk_xxx", "https://api.example.com");
zk.loginGuest("device-123", "").thenCompose(x -> zk.dataSet("save1", Map.of("level", 3))).join();
```

Failures complete exceptionally with `ZekumoException` (`status`, `code`).
License: MIT.
