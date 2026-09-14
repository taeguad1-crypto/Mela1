package com.melasarang.room;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VoiceCommandRouterTest {
    private final VoiceCommandRouter router = new VoiceCommandRouter();

    @Test public void opensProviderHomePages() {
        assertEquals("https://www.naver.com/", router.route("네이버 열어줘").value);
        assertEquals("https://www.google.com/", router.route("구글 열어줘").value);
        assertEquals("https://www.youtube.com/", router.route("유튜브 열어줘").value);
    }

    @Test public void routesProviderSearches() {
        VoiceCommandRouter.Action action = router.route("네이버에서 부산 맛집 검색해줘");
        assertEquals(VoiceCommandRouter.Type.OPEN_URL, action.type);
        assertTrue(action.value.startsWith("https://search.naver.com/"));
        assertTrue(action.value.contains("%EB%B6%80%EC%82%B0"));
    }

    @Test public void separatesHideFromFullStop() {
        assertEquals(VoiceCommandRouter.Type.HIDE_MIC, router.route("마이크 치워줘").type);
        assertEquals(VoiceCommandRouter.Type.STOP_MIC, router.route("마이크 꺼줘").type);
        assertEquals(VoiceCommandRouter.Type.SHOW_MIC, router.route("세온아 마이크 보여줘").type);
    }

    @Test public void routesNavigationAndTranslation() {
        assertEquals(VoiceCommandRouter.Type.BACK, router.route("이전 페이지").type);
        assertEquals(VoiceCommandRouter.Type.HOME, router.route("사랑방 홈으로").type);
        assertEquals(VoiceCommandRouter.Type.FORWARD, router.route("다음 페이지").type);
        assertEquals(VoiceCommandRouter.Type.TRANSLATE_PAGE, router.route("전체 화면 번역해줘").type);
    }

    @Test public void routesBusinessCategoriesInsidePortal() {
        assertEquals(VoiceCommandRouter.Type.PAGE_COMMAND, router.route("멜라루카 카탈로그 열어줘").type);
        assertEquals(VoiceCommandRouter.Type.PAGE_COMMAND, router.route("멜라루카 고객 관리 열어줘").type);
        assertEquals(VoiceCommandRouter.Type.PAGE_COMMAND, router.route("45포인트 추천 제품").type);
        assertEquals(VoiceCommandRouter.Type.PAGE_COMMAND, router.route("에센셜 오일 사용법").type);
    }
}
