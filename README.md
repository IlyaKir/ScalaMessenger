# ScalaMessenger

Для запуска вводим команду "docker-compose up" и после того как веб-сервер развернётся, можем делать к нему запросы. Контейнер с Postgres использует порт 5432, поэтому если postgres уже запущен, то он скорее всего использует этот порт.
Решение проблемы: sudo ss -lptn 'sport = :5432'
sudo kill pidId

PS в папке curl_requests есть готовые запросы, на которых можно проверить работоспособность сервиса

Для проверки:
1) заходим на localhost:9000/graphql
2) делаем запрос createUser с именем 'user1'
3) делаем запрос createUser с именем 'user2'
4) подключаемся через ws по адресу localhost:9000/graphql/subscribe (ws client: https://chrome.google.com/webstore/detail/web-socket-client/lifhekgaodigcpmnakfhaaaboididbdn?hl=en-US)
5) отправляем по ws сообщение из curl_requests/subscription (тем самым подписываемся на "createMessage" и "updateMessage" пользователя 'user1')
6) запускаем запрос createMessage_123test321 из папки curl_requests и видим что по ws приходит уведомление
7) запускаем запрос updateMessage_1_test из папки curl_requests и видим что по ws приходит уведомление
