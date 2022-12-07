## Structure

- Source code files, `Makefile`, and configuration files are in the folder `volumes/p2p`
- File `jdk-8u341-linux-x64.tar.gz` is needed to be downloaded from [website](https://www.oracle.com/java/technologies/javase/javase8u211-later-archive-downloads.html).

### Files Structure
```
.
├── docker-compose.yml
├── Dockerfile
├── jdk-8u341-linux-x64.tar.gz
└── volumes
    └── p2p
        ├── Common.cfg
        ├── config
        │   ├── Common.java
        │   └── PeerInfo.java
        ├── main
        │   ├── message
        │   │   ├── ActualMessage.java
        │   │   ├── HandshakeMessage.java
        │   │   ├── MessageType.java
        │   │   └── type
        │   │       ├── BitfieldMessage.java
        │   │       ├── ChokeMessage.java
        │   │       ├── EndMessage.java
        │   │       ├── HaveMessage.java
        │   │       ├── InterestedMessage.java
        │   │       ├── NotInterestedMessage.java
        │   │       ├── PieceMessage.java
        │   │       ├── RequestMessage.java
        │   │       └── UnchokeMessage.java
        │   ├── PeerConnection.java
        │   ├── PeerHandler.java
        │   ├── PeerHub.java
        │   ├── PeerLogger.java
        │   ├── PeerSelector.java
        │   └── piece
        │       ├── PieceReceiver.java
        │       └── PieceSender.java
        ├── Makefile
        ├── 1001
        │   └── thefile
        ├── 1006
        │   └── thefile
        ├── PeerInfo.cfg
        ├── peerProcess.java
        ├── ResultVerifier.java
        └── utils
            ├── BitfieldUtils.java
            └── PeerUtils.java
```

## How to Test on the UFL Linux Servers

1. Connect to UFL Wifi or VPN
2. `scp volumes.tar.gz _YOUR_UFL_USER_NAME_@storm.cise.ufl.edu:/cise/homes/_YOUR_UFL_USER_NAME_`
3. `ssh _YOUR_UFL_USER_NAME_@storm.cise.ufl.edu`
4. Enter your UFL account password
5. `tar -zxvf volumes.tar.gz`
6. `cd volumes/p2p`
7. `make clean && make`
8. Move initial files from `project_config_file_large` or `project_config_file_small` to `volumes/p2p/`
9. `java StartRemotePeers` or `sh StartRemotePeers.sh`
10. `java ResultVerifier` to check whether all downloaded files are same 

