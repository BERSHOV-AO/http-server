import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
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
                        // так как мы должны в
                        out.flush();
                        continue;
                    }



                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
