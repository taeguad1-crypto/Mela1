import com.melasarang.room.VoiceCommandRouter;

public final class RouterSmokeTest {
    private static int checks;

    public static void main(String[] args) {
        VoiceCommandRouter router = new VoiceCommandRouter();
        check(router.route("네이버 열어줘").value.equals("https://www.naver.com/"), "Naver home");
        check(router.route("구글 열어줘").value.equals("https://www.google.com/"), "Google home");
        check(router.route("유튜브 열어줘").value.equals("https://www.youtube.com/"), "YouTube home");
        check(router.route("부산 맛집 검색해줘").value.startsWith("https://search.naver.com/"), "default Naver search");
        check(router.route("마이크 치워줘").type == VoiceCommandRouter.Type.HIDE_MIC, "hide keeps recognition");
        check(router.route("마이크 꺼줘").type == VoiceCommandRouter.Type.STOP_MIC, "full stop differs from hide");
        check(router.route("마이크 보여줘").type == VoiceCommandRouter.Type.SHOW_MIC, "show microphone");
        check(router.route("이전 페이지").type == VoiceCommandRouter.Type.BACK, "back");
        check(router.route("사랑방 홈으로").type == VoiceCommandRouter.Type.HOME, "home");
        check(router.route("다음 페이지").type == VoiceCommandRouter.Type.FORWARD, "forward");
        check(router.route("전체 화면 번역해줘").type == VoiceCommandRouter.Type.TRANSLATE_PAGE, "translation");
        check(router.route("멜라루카 회원 혜택방 열어줘").type == VoiceCommandRouter.Type.PAGE_COMMAND, "Melaleuca room");
        check(router.route("멜라루카 고객 관리 열어줘").type == VoiceCommandRouter.Type.PAGE_COMMAND, "Melaleuca customer room");
        check(router.route("55포인트 추천 제품").type == VoiceCommandRouter.Type.PAGE_COMMAND, "recommended products");
        check(router.route("에센셜 오일 교육").type == VoiceCommandRouter.Type.PAGE_COMMAND, "essential oil class");
        System.out.println("멜라루카 사랑방 router smoke test: " + checks + " checks passed");
    }

    private static void check(boolean condition, String name) {
        checks++;
        if (!condition) throw new AssertionError("Failed: " + name);
    }
}
