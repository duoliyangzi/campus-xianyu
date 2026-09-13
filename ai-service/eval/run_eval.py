"""
校园咸鱼 AI 小评测集（含 easy / hard）

用法（在 ai-service 目录、已激活 venv）：
  python -m eval.run_eval
  python -m eval.run_eval --only intent
  python -m eval.run_eval --difficulty hard
  python -m eval.run_eval --json-out eval/last_report.json

指标：
  - 意图准确率 / 检索词命中
  - RAG Hit@1 / Hit@K
  - 风控一致率；REJECT 精确率/召回（must-reject）
  - 分 difficulty=easy|hard|all 汇报，避免只报容易集虚高
"""

from __future__ import annotations

import argparse
import json
import time
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parent
CASES_PATH = ROOT / "cases.json"


def _load_cases() -> dict[str, Any]:
    return json.loads(CASES_PATH.read_text(encoding="utf-8"))


def _as_set(value: Any) -> set[str]:
    if value is None:
        return set()
    if isinstance(value, list):
        return {str(x).upper() for x in value}
    return {str(value).upper()}


def _filter_difficulty(cases: list[dict[str, Any]], difficulty: str) -> list[dict[str, Any]]:
    if difficulty == "all":
        return cases
    return [c for c in cases if (c.get("difficulty") or "easy") == difficulty]


def _slice_metrics_intent(rows: list[dict[str, Any]]) -> dict[str, Any]:
    if not rows:
        return {"total": 0, "accuracy": None, "keyword_hit_rate": None}
    ok = sum(1 for r in rows if r["intent_ok"])
    kw_rows = [r for r in rows if r["keyword_ok"] is not None]
    kw_ok = sum(1 for r in kw_rows if r["keyword_ok"])
    return {
        "total": len(rows),
        "accuracy": round(ok / len(rows), 4),
        "keyword_hit_rate": round(kw_ok / len(kw_rows), 4) if kw_rows else None,
    }


def _slice_metrics_rag(rows: list[dict[str, Any]]) -> dict[str, Any]:
    if not rows:
        return {"total": 0, "hit_at_1": None, "hit_at_k": None}
    return {
        "total": len(rows),
        "hit_at_1": round(sum(1 for r in rows if r["hit@1"]) / len(rows), 4),
        "hit_at_k": round(sum(1 for r in rows if r["hit@k"]) / len(rows), 4),
    }


def _slice_metrics_risk(rows: list[dict[str, Any]]) -> dict[str, Any]:
    if not rows:
        return {"total": 0, "agreement": None, "reject_precision": None, "reject_recall": None}
    ok = sum(1 for r in rows if r["ok"])
    tp = fp = fn = 0
    for r in rows:
        expect = set(r["expect"])
        sug = r["pred"]
        expect_allows_reject = "REJECT" in expect
        must_reject = expect == {"REJECT"}
        pred_reject = sug == "REJECT"
        if pred_reject and expect_allows_reject:
            tp += 1
        elif pred_reject and not expect_allows_reject:
            fp += 1
        elif (not pred_reject) and must_reject:
            fn += 1
    precision = tp / (tp + fp) if (tp + fp) else None
    recall = tp / (tp + fn) if (tp + fn) else None
    return {
        "total": len(rows),
        "agreement": round(ok / len(rows), 4),
        "reject_precision": None if precision is None else round(precision, 4),
        "reject_recall": None if recall is None else round(recall, 4),
    }


def _by_difficulty(rows: list[dict[str, Any]], metric_fn) -> dict[str, Any]:
    easy = [r for r in rows if r.get("difficulty") == "easy"]
    hard = [r for r in rows if r.get("difficulty") == "hard"]
    return {
        "all": metric_fn(rows),
        "easy": metric_fn(easy),
        "hard": metric_fn(hard),
    }


def eval_intent(cases: list[dict[str, Any]]) -> dict[str, Any]:
    from app.graph_workflow import classify_user_intent

    rows: list[dict[str, Any]] = []
    t0 = time.perf_counter()
    for case in cases:
        pred = classify_user_intent(case["message"])
        intent_hit = pred.get("intent") == case["expect_intent"]
        keyword_hit = None
        expect_kw = case.get("expect_keyword_any") or []
        if expect_kw:
            got = str(pred.get("keyword") or "")
            keyword_hit = any(k in got for k in expect_kw)
        rows.append(
            {
                "id": case["id"],
                "difficulty": case.get("difficulty") or "easy",
                "message": case["message"],
                "expect": case["expect_intent"],
                "pred": pred.get("intent"),
                "keyword": pred.get("keyword"),
                "source": pred.get("source"),
                "intent_ok": intent_hit,
                "keyword_ok": keyword_hit,
            }
        )
    elapsed = time.perf_counter() - t0
    by = _by_difficulty(rows, _slice_metrics_intent)
    return {
        "name": "intent",
        "total": len(cases),
        "accuracy": by["all"]["accuracy"],
        "keyword_hit_rate": by["all"]["keyword_hit_rate"],
        "by_difficulty": by,
        "latency_s": round(elapsed, 2),
        "rows": rows,
    }


def eval_rag(cases: list[dict[str, Any]], top_k: int = 3) -> dict[str, Any]:
    from app.rag import get_rag

    rag = get_rag()
    rows: list[dict[str, Any]] = []
    t0 = time.perf_counter()
    for case in cases:
        hits = rag.retrieve(case["message"], top_k=top_k)
        needles = case.get("expect_any_in_content") or []
        texts = [(h.title or "") + "\n" + (h.content or "") for h in hits]
        top1 = texts[0] if texts else ""
        joined = "\n".join(texts)
        ok1 = any(n in top1 for n in needles) if needles else False
        okk = any(n in joined for n in needles) if needles else False
        rows.append(
            {
                "id": case["id"],
                "difficulty": case.get("difficulty") or "easy",
                "message": case["message"],
                "hit@1": ok1,
                "hit@k": okk,
                "top_titles": [h.title for h in hits],
                "backend": rag.backend,
            }
        )
    elapsed = time.perf_counter() - t0
    by = _by_difficulty(rows, _slice_metrics_rag)
    return {
        "name": "rag",
        "total": len(cases),
        "hit_at_1": by["all"]["hit_at_1"],
        "hit_at_k": by["all"]["hit_at_k"],
        "top_k": top_k,
        "by_difficulty": by,
        "latency_s": round(elapsed, 2),
        "rows": rows,
    }


def eval_risk(cases: list[dict[str, Any]]) -> dict[str, Any]:
    from app.risk_assess import assess_content_risk

    rows: list[dict[str, Any]] = []
    t0 = time.perf_counter()
    for case in cases:
        expect = _as_set(case.get("expect_suggestion"))
        result = assess_content_risk(case["message"])
        sug = str(result.get("suggestion") or "").upper()
        hit = sug in expect
        rows.append(
            {
                "id": case["id"],
                "difficulty": case.get("difficulty") or "easy",
                "message": case["message"],
                "expect": sorted(expect),
                "pred": sug,
                "risk_level": result.get("risk_level"),
                "ok": hit,
            }
        )
    elapsed = time.perf_counter() - t0
    by = _by_difficulty(rows, _slice_metrics_risk)
    return {
        "name": "risk",
        "total": len(cases),
        "agreement": by["all"]["agreement"],
        "reject_precision": by["all"]["reject_precision"],
        "reject_recall": by["all"]["reject_recall"],
        "by_difficulty": by,
        "latency_s": round(elapsed, 2),
        "rows": rows,
    }


def _fmt_pct(value: Any) -> str:
    if value is None:
        return "n/a"
    return f"{value:.2%}"


def _print_summary(report: dict[str, Any]) -> None:
    print("\n===== AI 小评测报告（easy / hard） =====")
    for key in ("intent", "rag", "risk"):
        block = report.get(key)
        if not block:
            continue
        by = block.get("by_difficulty") or {}
        if key == "intent":
            for level in ("all", "easy", "hard"):
                m = by.get(level) or {}
                print(
                    f"[intent:{level}] accuracy={_fmt_pct(m.get('accuracy'))} "
                    f"keyword_hit={m.get('keyword_hit_rate')} n={m.get('total')}"
                )
            print(f"  总耗时={block['latency_s']}s")
            for row in block["rows"]:
                if not row["intent_ok"]:
                    print(
                        f"  miss {row['id']}({row['difficulty']}): "
                        f"expect={row['expect']} pred={row['pred']} | {row['message']}"
                    )
        elif key == "rag":
            for level in ("all", "easy", "hard"):
                m = by.get(level) or {}
                print(
                    f"[rag:{level}] hit@1={_fmt_pct(m.get('hit_at_1'))} "
                    f"hit@{block['top_k']}={_fmt_pct(m.get('hit_at_k'))} n={m.get('total')}"
                )
            print(f"  总耗时={block['latency_s']}s")
            for row in block["rows"]:
                if not row["hit@k"]:
                    print(
                        f"  miss {row['id']}({row['difficulty']}): "
                        f"{row['message']} tops={row['top_titles']}"
                    )
        else:
            for level in ("all", "easy", "hard"):
                m = by.get(level) or {}
                print(
                    f"[risk:{level}] agreement={_fmt_pct(m.get('agreement'))} "
                    f"reject_P={m.get('reject_precision')} reject_R={m.get('reject_recall')} "
                    f"n={m.get('total')}"
                )
            print(f"  总耗时={block['latency_s']}s")
            for row in block["rows"]:
                if not row["ok"]:
                    print(
                        f"  miss {row['id']}({row['difficulty']}): "
                        f"expect={row['expect']} pred={row['pred']} | {row['message']}"
                    )
    print("======================================\n")


def main() -> None:
    parser = argparse.ArgumentParser(description="校园咸鱼 AI 小评测")
    parser.add_argument("--only", default="intent,rag,risk", help="逗号分隔：intent,rag,risk")
    parser.add_argument("--difficulty", default="all", choices=["all", "easy", "hard"])
    parser.add_argument("--json-out", default="", help="把完整报告写入该路径")
    parser.add_argument("--top-k", type=int, default=3)
    args = parser.parse_args()

    wanted = {x.strip() for x in args.only.split(",") if x.strip()}
    cases = _load_cases()
    report: dict[str, Any] = {
        "generated_at": time.strftime("%Y-%m-%d %H:%M:%S"),
        "difficulty_filter": args.difficulty,
    }

    if "intent" in wanted:
        report["intent"] = eval_intent(_filter_difficulty(cases.get("intent") or [], args.difficulty))
    if "rag" in wanted:
        report["rag"] = eval_rag(
            _filter_difficulty(cases.get("rag") or [], args.difficulty),
            top_k=args.top_k,
        )
    if "risk" in wanted:
        report["risk"] = eval_risk(_filter_difficulty(cases.get("risk") or [], args.difficulty))

    _print_summary(report)
    if args.json_out:
        out = Path(args.json_out)
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"已写入 {out}")


if __name__ == "__main__":
    main()
