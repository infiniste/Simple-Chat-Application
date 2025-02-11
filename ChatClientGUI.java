package chat; // 패키지 선언

import javax.swing.*; // GUI 관련 라이브러리
import javax.swing.text.DefaultCaret;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*; // GUI 레이아웃 관련
import java.awt.event.*; // 이벤트 처리 관련
import java.io.*; // 입출력 처리
import java.net.*; // 네트워크 소켓 처리
import java.text.SimpleDateFormat;
import java.util.Date; // 날짜 포맷 처리

public class ChatClientGUI {
    private static final String SERVER_ADDRESS = "localhost"; // 서버 주소 (로컬호스트)
    private static final int PORT = 12345; // 서버 포트 번호
    private static final int MAX_MESSAGE_LENGTH = 200; // ✅ 메시지 길이 제한

    private Socket socket; // 서버와 연결할 소켓
    private PrintWriter out; // 메시지 전송을 위한 출력 스트림
    private BufferedReader in; // 메시지 수신을 위한 입력 스트림
    private JFrame frame; // GUI 프레임
    private JTextArea chatArea; // 채팅창 (출력)
    private JTextField inputField; // 입력 필드 (사용자가 메시지를 입력하는 곳)
    private JButton sendButton, colorButton, renameButton, emojiButton; // UI 버튼들
    private JComboBox<String> roomSelector; // 채팅방 선택 드롭다운
    private String nickname; // 사용자 닉네임

    public ChatClientGUI() {
        setNickname(); // ✅ 닉네임 설정

        // ✅ GUI 창 설정
        frame = new JFrame("Java Chat - " + nickname);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 600);
        frame.setLayout(new BorderLayout());

        // ✅ 채팅창 설정 (검은색 배경)
        chatArea = new JTextArea();
        chatArea.setEditable(false); // 채팅창은 수정 불가능
        chatArea.setLineWrap(true); // 자동 줄바꿈 활성화
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Malgun Gothic", Font.PLAIN, 14)); // 폰트 설정
        chatArea.setBackground(Color.DARK_GRAY);
        chatArea.setForeground(Color.WHITE);

        // ✅ 채팅창 자동 스크롤 설정
        DefaultCaret caret = (DefaultCaret) chatArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JScrollPane chatScrollPane = new JScrollPane(chatArea); // 스크롤 추가
        frame.add(chatScrollPane, BorderLayout.CENTER);

        // ✅ 메시지 입력 패널 설정
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        inputField = new JTextField();
        inputField.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        sendButton = new JButton("Send"); // 메시지 전송 버튼
        sendButton.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));

        panel.add(inputField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);

        // ✅ 채팅방 선택 드롭다운 추가
        String[] rooms = {"일반", "게임", "스터디", "기타"};
        roomSelector = new JComboBox<>(rooms);
        frame.add(roomSelector, BorderLayout.NORTH);

        // ✅ 추가 기능 버튼 (배경색 변경, 닉네임 변경)
        JPanel buttonPanel = new JPanel();
        colorButton = new JButton("🎨 색상 변경");
        renameButton = new JButton("닉네임 변경");

        // ✅ 버튼 기능 추가
        colorButton.addActionListener(e -> changeChatColor()); // 배경색 변경 기능
        renameButton.addActionListener(e -> changeNickname()); // 닉네임 변경 기능

        buttonPanel.add(colorButton);
        buttonPanel.add(renameButton);

        frame.add(buttonPanel, BorderLayout.WEST);
        frame.add(panel, BorderLayout.SOUTH);
        frame.setVisible(true); // 창을 표시

        // ✅ 버튼 및 키 이벤트 설정
        sendButton.addActionListener(e -> sendMessage()); // 전송 버튼 클릭 시 메시지 전송
        inputField.addActionListener(e -> sendMessage()); // 입력 필드에서 Enter 키 입력 시 메시지 전송

        // ✅ Shift + Enter 줄바꿈 기능 추가
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (e.isShiftDown()) {
                        inputField.setText(inputField.getText() + "\n"); // Shift + Enter 줄바꿈
                    } else {
                        sendMessage();
                        e.consume(); // 기본 Enter 동작 방지
                    }
                }
            }
        });

        // ✅ 메시지 길이 제한 기능 추가
        inputField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { checkMessageLength(); }
            @Override
            public void removeUpdate(DocumentEvent e) { checkMessageLength(); }
            @Override
            public void changedUpdate(DocumentEvent e) {}
        });

        connectToServer(); // ✅ 서버에 연결
    }

    // ✅ 닉네임 설정
    private void setNickname() {
        nickname = JOptionPane.showInputDialog(null, "닉네임을 입력하세요:", "닉네임 설정", JOptionPane.PLAIN_MESSAGE);
        if (nickname == null || nickname.trim().isEmpty()) {
            nickname = "익명_" + (int) (Math.random() * 1000);
        }
    }

    // ✅ 서버 연결
    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            out.println(nickname); // 서버에 닉네임 전송

            // ✅ 메시지 수신 스레드 실행
            Thread readerThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        appendMessage(serverMessage);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            readerThread.start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "서버 연결 실패!", "오류", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    // ✅ 메시지 추가 (자동 스크롤 포함)
    private void appendMessage(String message) {
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    // ✅ 메시지 전송
    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            String timestamp = new SimpleDateFormat("HH:mm").format(new Date());
            String selectedRoom = (String) roomSelector.getSelectedItem();
            String formattedMessage = "[" + timestamp + "] " + nickname + ": " + message;
            out.println("[" + selectedRoom + "] " + formattedMessage);
            inputField.setText("");
        }
    }

    // ✅ 배경색 변경 기능
    private void changeChatColor() {
        Color newColor = JColorChooser.showDialog(frame, "배경 색상 선택", chatArea.getBackground());
        if (newColor != null) {
            chatArea.setBackground(newColor);
        }
    }

    // ✅ 닉네임 변경 기능
    private void changeNickname() {
        String newNickname = JOptionPane.showInputDialog(frame, "새 닉네임 입력:", "닉네임 변경", JOptionPane.PLAIN_MESSAGE);
        if (newNickname != null && !newNickname.trim().isEmpty()) {
            out.println("[닉네임 변경] " + nickname + " → " + newNickname);
            nickname = newNickname;
            frame.setTitle("Java Chat - " + nickname);
        }
    }

    // ✅ 메시지 길이 제한 체크
    private void checkMessageLength() {
        if (inputField.getText().length() > MAX_MESSAGE_LENGTH) {
            JOptionPane.showMessageDialog(frame, "메시지는 최대 " + MAX_MESSAGE_LENGTH + "자까지만 입력 가능합니다!", "알림", JOptionPane.WARNING_MESSAGE);
            inputField.setText(inputField.getText().substring(0, MAX_MESSAGE_LENGTH));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClientGUI::new);
    }
}
