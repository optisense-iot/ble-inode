# ble-inode

Manual end-to-end test:

```
$ mosquitto
```

```
$ ./mill server.run
```

```
$ mosquitto_sub -h localhost -t "processed" -q 1
```

```
$ mosquitto_pub -h localhost -p 1883 -t raw -f payload.bin
```
