#! /bin/sh

$(sudo docker exec -it p1-10.9.0.11 /bin/bash -c 'cd ./p2p && java peerProcess 11001')
$(sudo docker exec -it p2-10.9.0.12 /bin/bash -c 'cd ./p2p && java peerProcess 11002')
$(sudo docker exec -it p3-10.9.0.13 /bin/bash -c 'cd ./p2p && java peerProcess 11003')
$(sudo docker exec -it p4-10.9.0.14 /bin/bash -c 'cd ./p2p && java peerProcess 11004')
$(sudo docker exec -it p5-10.9.0.15 /bin/bash -c 'cd ./p2p && java peerProcess 11005')
$(sudo docker exec -it p6-10.9.0.16 /bin/bash -c 'cd ./p2p && java peerProcess 11006')
