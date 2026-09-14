package com.melasarang.room;

import java.net.URLEncoder;
import java.util.Locale;

/** Converts Korean speech into browser, page and floating-microphone actions. */
public final class VoiceCommandRouter {
    private static final String HOME = "https://melaleuca-room.kwonkiil.chatgpt.site/";

    public enum Type {
        OPEN_URL, BACK, FORWARD, HOME, RELOAD, SHOW_MIC, HIDE_MIC, STOP_MIC,
        MUTE_SPEECH, UNMUTE_SPEECH, TRANSLATE_PAGE, PAGE_COMMAND, UNKNOWN
    }

    public static final class Action {
        public final Type type;
        public final String value;
        public final String confirmation;

        private Action(Type type, String value, String confirmation) {
            this.type = type;
            this.value = value;
            this.confirmation = confirmation;
        }

        public static Action of(Type type, String confirmation) {
            return new Action(type, "", confirmation);
        }

        public static Action open(String url, String confirmation) {
            return new Action(Type.OPEN_URL, url, confirmation);
        }

        public static Action page(String command, String confirmation) {
            return new Action(Type.PAGE_COMMAND, command, confirmation);
        }
    }

    public Action route(String transcript) {
        String spoken = normalize(transcript);
        if (spoken.isEmpty()) return Action.of(Type.UNKNOWN, "");

        if (containsAny(spoken, "마이크꺼", "마이크종료", "음성인식꺼")) {
            return Action.of(Type.STOP_MIC, "마이크를 끕니다. 앱 아이콘이나 알림을 눌러 다시 켤 수 있습니다.");
        }
        if (containsAny(spoken, "마이크숨겨", "마이크치워", "마이크감춰")) {
            return Action.of(Type.HIDE_MIC, "마이크 표시를 숨깁니다. 음성 명령은 계속 듣습니다.");
        }
        if (containsAny(spoken, "마이크보여", "마이크나와", "마이크켜")) {
            return Action.of(Type.SHOW_MIC, "마이크를 표시합니다.");
        }
        if (containsAny(spoken, "음소거", "대답소리꺼", "말하지마")) {
            return Action.of(Type.MUTE_SPEECH, "");
        }
        if (containsAny(spoken, "소리켜", "대답해", "음성안내켜")) {
            return Action.of(Type.UNMUTE_SPEECH, "음성 안내를 켰습니다.");
        }
        if (containsAny(spoken, "사랑방홈", "우리화면", "메인화면", "홈으로", "홈복귀")) {
            return Action.of(Type.HOME, "사랑방 홈으로 이동합니다.");
        }
        if (containsAny(spoken, "이전페이지", "뒤로가기", "뒤로가", "이전으로")) {
            return Action.of(Type.BACK, "이전 페이지로 이동합니다.");
        }
        if (containsAny(spoken, "다음페이지", "앞으로가기", "앞으로가", "다음으로")) {
            return Action.of(Type.FORWARD, "다음 페이지로 이동합니다.");
        }
        if (containsAny(spoken, "새로고침", "다시불러")) {
            return Action.of(Type.RELOAD, "페이지를 새로 고칩니다.");
        }
        if (containsAny(spoken, "전체화면번역", "이페이지번역", "페이지번역", "화면번역")) {
            return Action.of(Type.TRANSLATE_PAGE, "현재 페이지를 한국어로 번역합니다.");
        }

        if (containsAny(spoken,
            "회원가입", "회원혜택", "사업오버뷰", "사업설명", "멜라라이프", "카탈로그",
            "추천제품", "제품선택", "포인트선택", "35포인트", "45포인트", "55포인트",
            "75포인트", "vip", "에센셜오일", "오일교육", "오일사용법", "오일브랜딩",
            "명함", "고객관리", "회원관리", "주문관리", "구독", "구독플랜",
            "AI상담", "회사문의", "법적안내", "보상플랜", "비즈니스연결", "사이트제작",
            "제품영상", "제품소개", "카테고리영상", "리뉴영상", "리뉴로션", "APK다운로드",
            "무료체험", "카카오채널", "카톡문의", "추가정보", "공개영상", "가입문의")) {
            return Action.page(transcript, "요청한 사랑방 메뉴를 엽니다.");
        }

        Action providerSearch = providerSearch(spoken, "네이버", "https://search.naver.com/search.naver?query=", "네이버에서 검색합니다.");
        if (providerSearch != null) return providerSearch;
        providerSearch = providerSearch(spoken, "구글", "https://www.google.com/search?q=", "구글에서 검색합니다.");
        if (providerSearch != null) return providerSearch;
        providerSearch = providerSearch(spoken, "유튜브", "https://www.youtube.com/results?search_query=", "유튜브에서 검색합니다.");
        if (providerSearch != null) return providerSearch;

        if (containsAny(spoken, "네이버열어", "네이버접속")) {
            return Action.open("https://www.naver.com/", "네이버를 전체 화면으로 엽니다.");
        }
        if (containsAny(spoken, "구글열어", "구글접속")) {
            return Action.open("https://www.google.com/", "구글을 전체 화면으로 엽니다.");
        }
        if (containsAny(spoken, "유튜브열어", "유튜브접속")) {
            return Action.open("https://www.youtube.com/", "유튜브를 전체 화면으로 엽니다.");
        }
        if (containsAny(spoken, "공식몰열어", "멜라루카공식", "공식사이트")) {
            return Action.open("https://kr.melaleuca.com/", "공식 사이트를 엽니다.");
        }
        if (containsAny(spoken, "사랑방열어", "우리사이트")) {
            return Action.open(HOME, "사랑방 홈을 엽니다.");
        }

        String generalQuery = extractGeneralSearch(transcript);
        if (!generalQuery.isEmpty()) {
            return Action.open(
                "https://search.naver.com/search.naver?query=" + encode(generalQuery),
                "네이버에서 " + generalQuery + " 검색을 시작합니다."
            );
        }
        return Action.of(Type.UNKNOWN, "");
    }

    private Action providerSearch(String normalized, String provider, String baseUrl, String confirmation) {
        if (!normalized.contains(provider) || !normalized.contains("검색")) return null;
        String query = normalized
            .replace(provider, "")
            .replace("에서", "")
            .replace("으로", "")
            .replace("검색해줘", "")
            .replace("검색해", "")
            .replace("검색", "")
            .trim();
        if (query.isEmpty()) return null;
        return Action.open(baseUrl + encode(query), confirmation);
    }

    private String extractGeneralSearch(String original) {
        if (original == null) return "";
        String result = original.trim();
        String[] suffixes = {"검색해 줘", "검색해줘", "검색해", "찾아 줘", "찾아줘", "찾아"};
        for (String suffix : suffixes) {
            if (result.endsWith(suffix)) {
                result = result.substring(0, result.length() - suffix.length()).trim();
                break;
            }
        }
        if (result.equals(original.trim())) return "";
        return result.replaceFirst("^(사랑방아|멜라루카야)\\s*", "").trim();
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.KOREA).replaceAll("[\\s,.!?·]", "");
    }

    private static boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) if (value.contains(candidate)) return true;
        return false;
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception impossibleOnAndroid) {
            return value;
        }
    }
}
