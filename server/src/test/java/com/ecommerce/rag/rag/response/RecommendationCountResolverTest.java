package com.ecommerce.rag.rag.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RecommendationCountResolverTest {

    private final RecommendationCountResolver resolver = new RecommendationCountResolver();

    @Test
    void recommendYiKuanRunningShoesShouldReturn1() {
        assertEquals(1, resolver.resolve("推荐一款跑鞋"));
    }

    @Test
    void recommendYiGeHeadphoneShouldReturn1() {
        assertEquals(1, resolver.resolve("推荐一个耳机"));
    }

    @Test
    void recommendYiShuangRunningShoesShouldReturn1() {
        assertEquals(1, resolver.resolve("推荐一双跑鞋"));
    }

    @Test
    void whichBestShouldReturn1() {
        assertEquals(1, resolver.resolve("哪款最适合跑步"));
    }

    @Test
    void recommendSeveralShouldReturn3() {
        assertEquals(3, resolver.resolve("推荐几款跑鞋"));
    }

    @Test
    void youNaXieShouldReturn3() {
        assertEquals(3, resolver.resolve("有哪些耳机"));
    }

    @Test
    void noExplicitCountShouldReturn3() {
        assertEquals(3, resolver.resolve("好用的跑鞋"));
    }

    @Test
    void nullQueryShouldReturn3() {
        assertEquals(3, resolver.resolve(null));
    }

    @Test
    void blankQueryShouldReturn3() {
        assertEquals(3, resolver.resolve("   "));
    }

    @Test
    void commaYiKuanShouldReturn1() {
        assertEquals(1, resolver.resolve("来一款跑鞋"));
    }

    @Test
    void mostRecommendedShouldReturn1() {
        assertEquals(1, resolver.resolve("最推荐跑鞋"));
    }

    @Test
    void giveMeSeveralChoicesShouldReturn3() {
        assertEquals(3, resolver.resolve("给我几个选择"));
    }
}
