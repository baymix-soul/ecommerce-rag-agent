package com.ecommerce.rag.rag.understanding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class CartSemanticFrameCatalog {

    private final List<CartSemanticFrame> frames;

    public CartSemanticFrameCatalog() {
        this.frames = buildCatalog();
    }

    private List<CartSemanticFrame> buildCatalog() {
        List<CartSemanticFrame> list = new ArrayList<>();

        CartSemanticFrame summary = new CartSemanticFrame();
        summary.setFrameId("cart.summary");
        summary.setDescription("用户想查看当前购物车内容、数量、总价、已买金额");
        summary.setIntent(QueryPlan.INTENT_CART_SUMMARY);
        summary.setCartAction("CART_SUMMARY");
        summary.setRequiredSlots(new ArrayList<>());
        summary.setPositiveExamples(List.of(
                "当前购物车多少钱了",
                "我已经买了多少钱",
                "购物车里有什么",
                "现在合计多少",
                "已经加购了哪些商品",
                "购物车总价多少"
        ));
        summary.setNegativeExamples(List.of(
                "推荐2000元以内的电脑",
                "这个商品多少钱"
        ));
        list.add(summary);

        CartSemanticFrame gapQuery = new CartSemanticFrame();
        gapQuery.setFrameId("cart.amount_gap_query");
        gapQuery.setDescription("用户想知道当前购物车金额距离某个目标金额还差多少");
        gapQuery.setIntent(QueryPlan.INTENT_CART_TOP_UP);
        gapQuery.setCartAction("AMOUNT_GAP_QUERY");
        gapQuery.setRequiredSlots(List.of("target_amount"));
        gapQuery.setPositiveExamples(List.of(
                "离2000还差多少",
                "距离2000还差多少",
                "到2000还差多少",
                "满2000还差多少",
                "还差多少到两千",
                "差多少到2000"
        ));
        gapQuery.setNegativeExamples(List.of(
                "推荐2000元以内的电脑",
                "预算2000买手机"
        ));
        list.add(gapQuery);

        CartSemanticFrame completionRec = new CartSemanticFrame();
        completionRec.setFrameId("cart.completion_recommend");
        completionRec.setDescription("用户想根据当前购物车金额凑到目标金额，并希望系统推荐商品");
        completionRec.setIntent(QueryPlan.INTENT_CART_TOP_UP);
        completionRec.setCartAction("COMPLETION_RECOMMEND");
        completionRec.setRequiredSlots(List.of("target_amount"));
        completionRec.setPositiveExamples(List.of(
                "如果要凑1000块，有没有推荐商品",
                "帮我凑到1000元",
                "想满2000，有什么可以加",
                "凑到两千推荐点东西"
        ));
        completionRec.setNegativeExamples(List.of(
                "推荐2000元以内的电脑",
                "2000块预算买什么好"
        ));
        list.add(completionRec);

        CartSemanticFrame completionClarify = new CartSemanticFrame();
        completionClarify.setFrameId("cart.completion_clarify");
        completionClarify.setDescription("用户想凑单，但没有给出目标金额");
        completionClarify.setIntent(QueryPlan.INTENT_CART_TOP_UP);
        completionClarify.setCartAction("COMPLETION_RECOMMEND");
        completionClarify.setRequiredSlots(new ArrayList<>());
        completionClarify.setPositiveExamples(List.of(
                "凑单推荐一下",
                "帮我凑单",
                "有什么适合凑单的"
        ));
        completionClarify.setNegativeExamples(List.of(
                "推荐凑单商品2000以内"
        ));
        list.add(completionClarify);

        CartSemanticFrame addItem = new CartSemanticFrame();
        addItem.setFrameId("cart.add_item");
        addItem.setDescription("用户想把当前商品或最近推荐商品加入购物车");
        addItem.setIntent(QueryPlan.INTENT_CART_TOP_UP);
        addItem.setCartAction("ADD_TO_CART");
        addItem.setRequiredSlots(new ArrayList<>());
        addItem.setPositiveExamples(List.of(
                "把这个加入购物车",
                "把第一款加到购物车",
                "买这个",
                "加购"
        ));
        addItem.setNegativeExamples(List.of());
        list.add(addItem);

        return Collections.unmodifiableList(list);
    }

    public List<CartSemanticFrame> getAllFrames() {
        return frames;
    }

    public Optional<CartSemanticFrame> getFrame(String frameId) {
        return frames.stream()
                .filter(f -> f.getFrameId().equals(frameId))
                .findFirst();
    }
}
