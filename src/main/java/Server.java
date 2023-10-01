import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class Server {

    public static void main(String[] args) {
        // validPaths список валидных путей, что бы указать на какие запросы сервер может ответить,
        // на какие ответить не может
        // Вместо var автоматически подставляется тип объекта
        final var validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html",
                "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
        // Используем серверный сокет ServerSocket, для того что бы мы могли слушать какой-то порт,
        // и принимать подключение
        try (final var serverSocket = new ServerSocket(9999)) {
            // Как и любой сервер, сервер должен работать в бесконечном цикле while (true)
            // Что бы сервер работал всегда
            while (true) {
                try (
                        // Блокирующее ожидание в serverSocket.accept();
                        // Ждем когда клиент подключится
                        // Как только клиент подключиться, будет создаваться клиентский сокет socket
                        final var socket = serverSocket.accept();
                        // И чтобы взаимодействовать с клиентским сокетом создаем два потока in, out
                        // Из in читаем данные
                        // Так как мы знаем что у нас будут строки, getInputStream() и в конечном итоге
                        // мы будем использовать BufferedReader
                        final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        // Выходной поток out у нас будет именно массив байт, здесь мы уже не сможем доверить работу
                        // со строками, надо отвечать максимально точно с точки зрения протокола, по этому,
                        // выходной поток мы будем писать в массив байт, когда мы будем отдавать клиенту,
                        // будем собирать строку, но когда мы ее будем отдавать клиенту то будем ее
                        // преобразовывать в массив байт
                        final var out = new BufferedOutputStream(socket.getOutputStream());
                ) {
                    // read only request line for simplicity
                    // must be in form GET /path HTTP/1.1
                    // GET /path HTTP/1.1 -> что важно с точки зрения протокола -> эта строка всегда приходит первой
                    // всегда три элемента именно в такой последовательности, соответственно эти элементы между собой
                    // разделены пробелами
                    final var requestLine = in.readLine();
                    // Данную строку мы разбиваем три части по пробелу
                    final var parts = requestLine.split(" ");

                    // И проверяем сколько частей получилось
                    if (parts.length != 3) {
                        // если не три части, просто закрываем сокет
                        // continue -> так как у нас бесконечный цикл, continue выбрасывает нас на следующую операцию
                        // Выходим из блока try, и клиентский сокет закрывается.
                        continue;
                    }
                    // Проверяем валидный путь, сравниваем пришедший путь с validPaths
                    // "Content-Length: 0\r\n" говорит о том, что нет тела ответа
                    // "Connection: close\r\n" соединение можно закрывать
                    final var path = parts[1];
                    if (!validPaths.contains(path)) {
                        out.write((
                                "HTTP/1.1 404 Not Found\r\n" +
                                        "Content-Length: 0\r\n" +
                                        "Connection: close\r\n" +
                                        "\r\n"
                        ).getBytes());
                        // Так как, мы должны в вернуть не строкой а массивом байт, мы ее не просто записываем в буфер
                        // мы из строки получаем массив байт с помощью getBytes(), и массив байт мы записываем в буфер
                        // При этом, если вдруг у нас данные будут долго отправляться, или буфер будет слишком большой,
                        // мы используем метод flush(), что бы принудительно буфер очисть, что бы убедится что клиену
                        // данные отправлены.
                        out.flush();
                        // Только после этого мы выходим на следующую итерацию continue.
                        continue;
                    }
                    // Сначала собираем путь к файлу на диске -> если путь правильный (пример: /index.html),
                    // то из этого пути, нужно установить путь файла на
                    // диске, это мы сможем сделать с помощью класс Path и статического конструктора of, так как файлы
                    // лежат в папке "public", а паблик лежит в корне проекта ".", таким образом мы собираем
                    // путь к файлу Path.of(".", "public", path);
                    final var filePath = Path.of(".", "public", path);
                    // Еще нужно указать тип контента, которы можно указать с помощью Files.probeContentType(filePath)
                    // ТО ЕСТЬ - это тип файла
                    final var mimeType = Files.probeContentType(filePath);

                    // TODO: 30.09.2023
                    // Если у нас приходит запрос на страничку "/classic.html"
                    if (path.equals("/classic.html")) {
                        // Мы читаем файл не как файл, читаем как строку
                        final var template = Files.readString(filePath);
                        // Называется отрендерить шаблон, находим в файле нашу метку "{time}", и заменяем ее на
                        // значение текущего времени LocalDateTime.now().toString()
                        // Соответственно получаем контент, и так как нам принято возвращать массив байт, приводим
                        // контент к массиву байт
                        final var content = template.replace(
                                "{time}", LocalDateTime.now().toString()
                        ).getBytes();
                        out.write((
                                "HTTP/1.1 200 OK\r\n" +
                                        "Content-Type: " + mimeType + "\r\n" +
                                        "Content-Length: " + content.length + "\r\n" +
                                        "Connection: close\r\n" +
                                        "\r\n"
                        ).getBytes());
                        out.write(content);
                        out.flush();
                        continue;
                    }
                    // Так как файл мы будем возвращать в теле ответа
                    // Возвращаем ти файла "Content-Type: " + mimeType + "\r\n" +
                    // Возвращаем размер файла "Content-Length: " + length + "\r\n" +
                    // Зарываем соединение "Connection: close\r\n" +
                    final var length = Files.size(filePath);
                    // формируем ответ клиенту, все это опять упаковываем в баты.
                    out.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: " + mimeType + "\r\n" +
                                    //"Content-Type: " + "text/plain" + "\r\n" +
                                    "Content-Length: " + length + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    // Весь этот ответ уходит в запись, что бы отправить тело ответа клиенту,
                    // это можно сделать с помощью утилиты работы с файлами с помощью класса Files и метода copy()
                    // мы указываем путь к файлу (filePath) и буфер (out) куда это путь
                    // нужно скопировать это можно скопировать
                    Files.copy(filePath, out);
                    // flush обязателен, по тому как файл может быть большой, и может передаваться долго
                    out.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
