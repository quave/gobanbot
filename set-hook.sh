#! /bin/bash

curl -H "Content-Type: multipart/form-data" \
	-X POST \
	-F "url=https://vladsynkov.ru:8443/api/update" \
	-F "certificate=@./gobanbot.pem" \
	https://api.telegram.org/bot162694958:AAGu9QiYPEm9ADwSYtEGEZ83G_9420ZvWok/setWebhook
