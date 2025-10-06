# SSL Error Fix Options for MongoDB Atlas

## Problem: javax.net.ssl.SSLException: Received fatal alert: internal_error

## Solutions to try:

### 1. Update Connection String (Already Done ✅)
```
spring.data.mongodb.uri=mongodb+srv://slthrmanager:ZP0NBGEEmKzjMkFl@cluster0.xpc7gfy.mongodb.net/tire_management?retryWrites=true&w=majority&tlsAllowInvalidHostnames=true
```

### 2. Run with JVM SSL Arguments:
```bash
./mvnw spring-boot:run -Dcom.mongodb.MongoClientSettings.socketSettings.enableTcpKeepAlive=true -Djavax.net.ssl.trustStore="path/to/truststore" -Djavax.net.ssl.trustStorePassword="password"
```

### 3. Alternative Connection String Options:
```
# Option A: Disable SSL verification (NOT for production)
mongodb+srv://slthrmanager:ZP0NBGEEmKzjMkFl@cluster0.xpc7gfy.mongodb.net/tire_management?ssl=false

# Option B: Use TLS settings
mongodb+srv://slthrmanager:ZP0NBGEEmKzjMkFl@cluster0.xpc7gfy.mongodb.net/tire_management?tls=true&tlsAllowInvalidCertificates=true

# Option C: Use specific TLS version
mongodb+srv://slthrmanager:ZP0NBGEEmKzjMkFl@cluster0.xpc7gfy.mongodb.net/tire_management?tls=true&tlsInsecure=true
```

### 4. Check MongoDB Atlas Settings:
- IP Whitelist: Add 0.0.0.0/0 for testing
- Database User: Verify slthrmanager has proper permissions
- Network Access: Check firewall settings

### 5. Java Version Issues:
Sometimes Java 17 has SSL compatibility issues. Try running with:
```bash
./mvnw spring-boot:run -Djava.net.useSystemProxies=true -Dhttps.protocols=TLSv1.2
```

### 6. Alternative - Use MongoDB Compass to test:
1. Download MongoDB Compass
2. Test connection with: mongodb+srv://slthrmanager:ZP0NBGEEmKzjMkFl@cluster0.xpc7gfy.mongodb.net/
3. If Compass works, the issue is with Java SSL configuration

## Quick Test Commands:
```bash
# Test 1: Run with SSL debugging
./mvnw spring-boot:run -Djavax.net.debug=ssl

# Test 2: Run with relaxed SSL
./mvnw spring-boot:run -Dtrust_all_cert=true

# Test 3: Use different TLS version
./mvnw spring-boot:run -Dhttps.protocols=TLSv1.2
```
