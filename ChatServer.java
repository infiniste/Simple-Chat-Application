package chat; // 패키지 선언

import java.io.*; // 입출력 관련 라이브러리
import java.net.*; // 네트워크 관련 라이브러리
import java.util.*; // 자료구조 및 유틸리티 라이브러리

public class ChatServer {
    private static final int PORT = 12345; // ✅ 서버 포트 번호 설정
    private static Set<PrintWriter> clientWriters = new HashSet<>(); // ✅ 연결된 클라이언트의 출력 스트림 리스트
    private static Set<String> users = new HashSet<>(); // ✅ 참가자 목록 (닉네임 저장)

    public static void main(String[] args) {
        System.out.println("채팅 서버 시작..."); // ✅ 서버 시작 메시지 출력

        try (ServerSocket serverSocket = new ServerSocket(PORT)) { // ✅ 서버 소켓 생성
            while (true) {
                new ClientHandler(serverSocket.accept()).start(); // ✅ 클라이언트 연결 시 새 쓰레드 실행
            }
        } catch (IOException e) {
            e.printStackTrace(); // ✅ 예외 발생 시 오류 출력
        }
    }

    // ✅ 클라이언트 관리 클래스 (각 클라이언트마다 쓰레드 실행)
    private static class ClientHandler extends Thread {
        private Socket socket; // ✅ 클라이언트와 연결된 소켓
        private PrintWriter out; // ✅ 클라이언트로 메시지를 보내기 위한 출력 스트림
        private BufferedReader in; // ✅ 클라이언트로부터 메시지를 받기 위한 입력 스트림
        private String userName; // ✅ 클라이언트 닉네임 저장 변수

        public ClientHandler(Socket socket) {
            this.socket = socket; // ✅ 소켓 초기화
        }

        public void run() {
            try {
                // ✅ 입력 및 출력 스트림 초기화 (UTF-8 인코딩)
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

                // ✅ 클라이언트 리스트에 출력 스트림 추가 (동기화하여 안전하게 저장)
                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                // ✅ 클라이언트로부터 닉네임 수신
                userName = in.readLine();
                users.add(userName); // ✅ 닉네임 리스트에 추가

                // ✅ 참가자 목록 업데이트 및 전체 클라이언트에게 전송
                broadcastUserList();
                broadcast("✅ " + userName + " 님이 입장하셨습니다.");

                String message;
                // ✅ 클라이언트가 보낸 메시지를 수신 및 브로드캐스트
                while ((message = in.readLine()) != null) {
                    broadcast(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    // ✅ 클라이언트 연결 종료 시 처리
                    socket.close();
                    synchronized (clientWriters) {
                        clientWriters.remove(out); // ✅ 클라이언트 리스트에서 제거
                    }
                    users.remove(userName); // ✅ 사용자 목록에서 제거
                    
                    // ✅ 참가자 목록 업데이트 및 클라이언트에게 전송
                    broadcastUserList();
                    broadcast("❌ " + userName + " 님이 퇴장하셨습니다.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // ✅ 모든 클라이언트에게 메시지 전송 (브로드캐스트)
        private void broadcast(String message) {
            synchronized (clientWriters) { // ✅ 동기화하여 여러 쓰레드에서 안전하게 실행
                for (PrintWriter writer : clientWriters) {
                    writer.println(message); // ✅ 메시지 전송
                }
            }
        }

        // ✅ 참가자 목록을 모든 클라이언트에게 전송
        private void broadcastUserList() {
            String userListString = "[USER_LIST] " + String.join(",", users); // ✅ 닉네임 리스트 문자열 생성
            synchronized (clientWriters) { // ✅ 동기화하여 안전하게 실행
                for (PrintWriter writer : clientWriters) {
                    writer.println(userListString); // ✅ 참가자 목록 전송
                }
            }
        }
    }
}

