mkdir -p /home/ec2-user/src/gobanbot
cd /home/ec2-user/src/gobanbot
#cp data.sqlite3 bak/data.dep.`date "+%Y%m%d-%H%M"`.sqlite3
git pull
docker build -t vladsynkov/gobanbot .
docker rm -f gobanbot
docker run -p 8443:8443 -d --name gobanbot vladsynkov/gobanbot
docker ps -a

# sshaws "sh /home/ec2-user/src/gobanbot/redeploy.sh"
