## Structure

- Files about the test based on Docker.

- Source code files, Makefile, and configuration files are in the folder volumes/p2p

- File jdk-8u341-linux-x64.tar.gz is needed to be downloaded from website.

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
        ├── peer_1001
        │   └── thefile
        ├── peer_1006
        │   └── thefile
        ├── PeerInfo.cfg
        ├── peerProcess.java
        ├── ResultVerifier.java
        └── utils
            ├── BitfieldUtils.java
            └── PeerUtils.java
```

## How to Test on the UFL Linux Servers

- Pull down all files this branch in to a folder.

- Run `sudo docker build -t jdk8:base .` in the folder same as the first step.

- Run `make clean && make` in the folder `volumes/p2p`.

- Run `sudo docker-compose up` in the folder same as the first step.

- There will be a huge amount of log information printed. No worry.
  - After all peers download the specific file and detect others also successfully download, all docker containers will stop automatically.
  - Run `java ResultVerifier` in the folder `volumes/p2p` to check whether all downloaded files are same.

- In the folder `volumes/p2p`, you can check log information of each peer.
