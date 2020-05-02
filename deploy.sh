#!/usr/bin/env bash

set -e

docker exec -it zimbradev_zimbra_1 bash -c "mkdir -p /opt/zimbra/lib/ext/rushfiles"
docker cp ./out/artifacts/jar/rushfiles.jar zimbradev_zimbra_1:/opt/zimbra/lib/ext/rushfiles/rushfiles.jar
docker exec -it zimbradev_zimbra_1 bash -c "su - zimbra -c 'zmmailboxdctl restart'"
docker exec -it zimbradev_zimbra_1 bash -c "cat /opt/zimbra/log/mailbox.log | grep -i rushfiles"