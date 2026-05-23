"""
One-time data conversion script: transforms teacher's raw e-commerce dataset
into standardized products.json + copies product images.

Usage:
    python scripts/convert_teacher_dataset.py \
        --input "E:\ecommerce_agent_dataset_供参考\ecommerce_agent_dataset" \
        --output "server/src/main/resources/data/products.json" \
        --images-output "server/src/main/resources/static/images"
"""

import argparse
import json
import os
import re
import shutil
import sys
from pathlib import Path


def clean_brand(brand: str) -> str:
    if not brand:
        return brand
    patterns = [
        (r"Apple\s+苹果", "Apple"),
        (r"Nike\s+耐克", "Nike"),
        (r"Adidas\s+阿迪达斯", "Adidas"),
        (r"Samsung\s+三星", "Samsung"),
        (r"Sony\s+索尼", "Sony"),
        (r"Lenovo\s+联想", "Lenovo"),
        (r"Philips\s+飞利浦", "Philips"),
        (r"Uniqlo\s+优衣库", "Uniqlo"),
        (r"L'Oreal\s+欧莱雅", "L'Oreal"),
    ]
    for pattern, replacement in patterns:
        if re.match(pattern, brand, re.IGNORECASE):
            return replacement
    return brand


def compute_price_range(skus: list) -> str:
    if not skus:
        return ""
    prices = []
    for sku in skus:
        p = sku.get("price")
        if p is not None:
            prices.append(float(p))
    if not prices:
        return ""
    min_p = min(prices)
    max_p = max(prices)
    if min_p == max_p:
        if min_p == int(min_p):
            return str(int(min_p))
        return str(min_p)
    else:
        min_str = str(int(min_p)) if min_p == int(min_p) else str(min_p)
        max_str = str(int(max_p)) if max_p == int(max_p) else str(max_p)
        return f"{min_str}~{max_str}"


def merge_specs(skus: list) -> dict:
    merged = {}
    for sku in skus:
        props = sku.get("properties", {})
        for key, value in props.items():
            if key not in merged:
                merged[key] = []
            if value not in merged[key]:
                merged[key].append(value)
    result = {}
    for key, values in merged.items():
        result[key] = "、".join(values)
    return result


def compute_avg_rating(reviews: list) -> float:
    if not reviews:
        return 0.0
    ratings = [r.get("rating", 0) for r in reviews if r.get("rating") is not None]
    if not ratings:
        return 0.0
    return round(sum(ratings) / len(ratings), 1)


def find_image_for_product(images_dir: Path, product_id: str) -> Path | None:
    if not images_dir.exists():
        return None
    for img in images_dir.iterdir():
        if img.is_file() and img.suffix.lower() in (".jpg", ".jpeg", ".png"):
            if product_id in img.stem:
                return img
    return None


def validate_product(product: dict, index: int) -> list[str]:
    warnings = []
    if not product.get("product_id"):
        warnings.append(f"Item {index}: missing product_id, skipping")
    if not product.get("name"):
        warnings.append(f"Item {index} ({product.get('product_id', '?')}): missing name, skipping")
    if not product.get("category"):
        warnings.append(f"Item {index} ({product.get('product_id', '?')}): missing category, skipping")
    if not product.get("sub_category"):
        warnings.append(f"Item {index} ({product.get('product_id', '?')}): missing sub_category, skipping")
    if not product.get("price") or product.get("price", 0) <= 0:
        warnings.append(f"Item {index} ({product.get('product_id', '?')}): invalid price, skipping")
    if not product.get("description"):
        warnings.append(f"Item {index} ({product.get('product_id', '?')}): missing description, skipping")
    if not product.get("image_url"):
        warnings.append(f"Item {index} ({product.get('product_id', '?')}): missing image_url, skipping")
    return warnings


def convert_dataset(input_dir: str, output_path: str, images_output_dir: str):
    input_path = Path(input_dir)
    output_file = Path(output_path)
    images_out = Path(images_output_dir)

    if not input_path.exists():
        print(f"ERROR: Input directory does not exist: {input_dir}")
        sys.exit(1)

    output_file.parent.mkdir(parents=True, exist_ok=True)
    images_out.mkdir(parents=True, exist_ok=True)

    products = []
    all_warnings = []
    image_count = 0
    seen_ids = set()

    for category_dir in sorted(input_path.iterdir()):
        if not category_dir.is_dir():
            continue

        data_dir = category_dir / "data"
        images_dir = category_dir / "images"

        if not data_dir.exists():
            print(f"WARNING: No data/ directory in {category_dir.name}, skipping")
            continue

        json_files = sorted(data_dir.glob("*.json"))
        print(f"Processing {category_dir.name}: {len(json_files)} JSON files")

        for json_file in json_files:
            try:
                with open(json_file, "r", encoding="utf-8") as f:
                    raw = json.load(f)
            except (json.JSONDecodeError, UnicodeDecodeError) as e:
                all_warnings.append(f"Failed to parse {json_file.name}: {e}")
                continue

            product_id = raw.get("product_id", "")
            if product_id in seen_ids:
                all_warnings.append(f"Duplicate product_id: {product_id}, skipping {json_file.name}")
                continue
            seen_ids.add(product_id)

            skus = raw.get("skus", [])
            rag = raw.get("rag_knowledge", {})
            reviews = rag.get("user_reviews", [])

            product = {
                "product_id": product_id,
                "name": raw.get("title", ""),
                "brand": clean_brand(raw.get("brand", "")),
                "category": raw.get("category", ""),
                "sub_category": raw.get("sub_category", ""),
                "price": raw.get("base_price", 0),
                "price_range": compute_price_range(skus),
                "image_url": f"/images/{product_id}.jpg",
                "description": rag.get("marketing_description", ""),
                "specs": merge_specs(skus),
                "avg_rating": compute_avg_rating(reviews),
                "currency": "CNY",
            }

            warnings = validate_product(product, len(products) + 1)
            if any("skipping" in w for w in warnings):
                all_warnings.extend(warnings)
                continue
            all_warnings.extend(warnings)

            img_src = find_image_for_product(images_dir, product_id)
            if img_src is None:
                all_warnings.append(
                    f"Item {product_id}: image file not found in {images_dir}"
                )

            products.append(product)

            if img_src is not None:
                img_dst = images_out / f"{product_id}.jpg"
                shutil.copy2(img_src, img_dst)
                image_count += 1

    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(products, f, ensure_ascii=False, indent=2)

    print(f"\n=== Conversion Summary ===")
    print(f"Total products converted: {len(products)}")
    print(f"Total images copied: {image_count}")
    print(f"Output JSON: {output_file}")
    print(f"Output images: {images_out}")

    if all_warnings:
        print(f"\n=== Warnings ({len(all_warnings)}) ===")
        for w in all_warnings:
            print(f"  - {w}")

    if len(products) == 0:
        print("\nERROR: No products were converted. Check input directory.")
        sys.exit(1)


def main():
    parser = argparse.ArgumentParser(
        description="Convert teacher's e-commerce dataset to standardized products.json"
    )
    parser.add_argument(
        "--input",
        required=True,
        help="Path to the teacher's dataset root directory",
    )
    parser.add_argument(
        "--output",
        required=True,
        help="Output path for the standardized products.json",
    )
    parser.add_argument(
        "--images-output",
        required=True,
        help="Output directory for product images",
    )
    args = parser.parse_args()
    convert_dataset(args.input, args.output, args.images_output)


if __name__ == "__main__":
    main()
