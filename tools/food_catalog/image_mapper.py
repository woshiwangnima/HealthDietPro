#!/usr/bin/env python3
"""Interactive local image-to-food mapping tool."""

import json
import tkinter as tk
from pathlib import Path
from tkinter import messagebox, ttk

ROOT = Path(__file__).resolve().parents[2]
SOURCE_DIR = ROOT / "tools/food_catalog/images/source"
MAPPING_FILE = SOURCE_DIR / "mapping.json"
SOURCE_RECORDS = ROOT / "tools/food_catalog/source/records"
IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}
KIND_LABELS = {"ingredient": "食材", "food": "食物", "dish": "菜肴"}


def load_foods():
    return [json.loads(path.read_text(encoding="utf-8")) for path in sorted(SOURCE_RECORDS.glob("*/*.json"))]


def load_mapping():
    if not MAPPING_FILE.is_file():
        return {}
    value = json.loads(MAPPING_FILE.read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise ValueError("mapping.json must contain an object")
    return {str(key): str(food_id) for key, food_id in value.items()}


class ImageMapper:
    def __init__(self, root):
        self.root = root
        self.foods = load_foods()
        self.mapping = load_mapping()
        all_images = [
            path for path in sorted(SOURCE_DIR.iterdir())
            if path.is_file() and path.suffix.lower() in IMAGE_SUFFIXES and path.name != MAPPING_FILE.name
        ]
        self.images = [path for path in all_images if path.name not in self.mapping]
        self.current_index = 0
        self.filtered_foods = []
        self.photo = None
        self.search_var = tk.StringVar()
        self.root_category_var = tk.StringVar(value="全部一级分类")
        self.child_category_var = tk.StringVar(value="全部二级分类")
        self.status_var = tk.StringVar()
        self.selection_var = tk.StringVar(value="未选择食物")
        self.build_ui()
        self.refresh_foods()
        self.refresh_image()

    def build_ui(self):
        self.root.title("食品图片映射")
        self.root.geometry("1180x720")
        self.root.minsize(900, 600)

        outer = ttk.Frame(self.root, padding=12)
        outer.pack(fill=tk.BOTH, expand=True)
        outer.columnconfigure(1, weight=1)
        outer.rowconfigure(1, weight=1)

        ttk.Label(outer, text="源图片").grid(row=0, column=0, sticky="w", padx=(0, 12))
        ttk.Label(outer, text="选择食材 / 食物 / 菜肴").grid(row=0, column=1, sticky="w")

        image_panel = ttk.Frame(outer, width=430)
        image_panel.grid(row=1, column=0, sticky="nsew", padx=(0, 12))
        image_panel.grid_propagate(False)
        image_panel.rowconfigure(0, weight=1)
        image_panel.columnconfigure(0, weight=1)
        self.image_label = ttk.Label(image_panel, anchor="center")
        self.image_label.grid(row=0, column=0, sticky="nsew")
        ttk.Label(image_panel, textvariable=self.status_var, anchor="center").grid(row=1, column=0, sticky="ew", pady=(8, 0))

        food_panel = ttk.Frame(outer)
        food_panel.grid(row=1, column=1, sticky="nsew")
        food_panel.columnconfigure(0, weight=1)
        food_panel.rowconfigure(2, weight=1)

        filters = ttk.Frame(food_panel)
        filters.grid(row=0, column=0, sticky="ew", pady=(0, 8))
        filters.columnconfigure(0, weight=1)
        ttk.Entry(filters, textvariable=self.search_var).grid(row=0, column=0, sticky="ew", padx=(0, 8))
        self.search_var.trace_add("write", lambda *_: self.refresh_foods())
        self.root_category_box = ttk.Combobox(filters, textvariable=self.root_category_var, state="readonly", width=18)
        self.root_category_box.grid(row=0, column=1, sticky="e", padx=(0, 6))
        self.root_category_box.bind("<<ComboboxSelected>>", self.root_category_selected)
        self.child_category_box = ttk.Combobox(filters, textvariable=self.child_category_var, state="readonly", width=22)
        self.child_category_box.grid(row=0, column=2, sticky="e")
        self.child_category_box.bind("<<ComboboxSelected>>", lambda _: self.refresh_foods())

        ttk.Label(food_panel, textvariable=self.selection_var).grid(row=1, column=0, sticky="w", pady=(0, 6))
        list_frame = ttk.Frame(food_panel)
        list_frame.grid(row=2, column=0, sticky="nsew")
        list_frame.rowconfigure(0, weight=1)
        list_frame.columnconfigure(0, weight=1)
        self.food_list = tk.Listbox(list_frame, exportselection=False)
        self.food_list.grid(row=0, column=0, sticky="nsew")
        scrollbar = ttk.Scrollbar(list_frame, orient="vertical", command=self.food_list.yview)
        scrollbar.grid(row=0, column=1, sticky="ns")
        self.food_list.configure(yscrollcommand=scrollbar.set)
        self.food_list.bind("<<ListboxSelect>>", self.food_selected)

        controls = ttk.Frame(outer)
        controls.grid(row=2, column=0, columnspan=2, sticky="ew", pady=(12, 0))
        controls.columnconfigure(2, weight=1)
        ttk.Button(controls, text="跳过此图片", command=self.skip_image).grid(row=0, column=0, padx=(0, 8))
        ttk.Button(controls, text="保存映射并下一张", command=self.save_and_next).grid(row=0, column=1, padx=(0, 8))
        ttk.Button(controls, text="退出", command=self.root.destroy).grid(row=0, column=3)

    def refresh_foods(self):
        query = "".join(self.search_var.get().lower().split())
        root_category = self.root_category_var.get()
        child_category = self.child_category_var.get()
        root_tag = self.category_tag(root_category)
        child_tag = self.category_tag(child_category)
        self.refresh_category_values(root_tag)
        self.filtered_foods = []
        self.food_list.delete(0, tk.END)
        mapped_ids = set(self.mapping.values())
        for food in self.foods:
            if food["id"] in mapped_ids:
                continue
            names = [name for values in food.get("names", {}).values() for name in values]
            searchable = "".join(" ".join(names).lower().split())
            if query and query not in searchable and query not in food["id"].lower().replace(":", "_"):
                continue
            tags = food.get("categoryTags", [])
            if root_tag and not any(tag == root_tag or tag.startswith(root_tag + ".") for tag in tags):
                continue
            if child_tag and not any(tag == child_tag or tag.startswith(child_tag + ".") for tag in tags):
                continue
            self.filtered_foods.append(food)
            kind = KIND_LABELS.get(food.get("kind", "ingredient"), food.get("kind", "ingredient"))
            label = f"[{kind}] {names[0] if names else food['id']}  |  {food['id']}"
            self.food_list.insert(tk.END, label)

    def category_tag(self, label):
        if label.startswith("全部"):
            return ""
        return label.split(" | ", 1)[-1]

    def category_label(self, tag):
        return f"{tag.rsplit('.', 1)[-1]} | {tag}"

    def root_tags(self):
        return sorted({tag.split(".")[0] + "." + tag.split(".")[1] for food in self.foods for tag in food.get("categoryTags", []) if tag.count(".") >= 1})

    def refresh_category_values(self, selected_root=None):
        roots = self.root_tags()
        root_values = ["全部一级分类"] + [self.category_label(tag) for tag in roots]
        if list(self.root_category_box["values"]) != root_values:
            self.root_category_box["values"] = root_values
        root_tag = selected_root or self.category_tag(self.root_category_var.get())
        if root_tag:
            children = sorted({tag for food in self.foods for tag in food.get("categoryTags", []) if tag.startswith(root_tag + ".")})
        else:
            children = sorted({tag for food in self.foods for tag in food.get("categoryTags", []) if tag.count(".") >= 2})
        child_values = ["全部二级分类"] + [self.category_label(tag) for tag in children]
        self.child_category_box["values"] = child_values
        if self.child_category_var.get() not in child_values:
            self.child_category_var.set("全部二级分类")

    def root_category_selected(self, _event=None):
        self.child_category_var.set("全部二级分类")
        self.refresh_category_values(self.category_tag(self.root_category_var.get()))
        self.refresh_foods()

    def food_selected(self, _event=None):
        selected = self.food_list.curselection()
        if selected:
            food = self.filtered_foods[selected[0]]
            self.selection_var.set(f"已选择：{food.get('names', {}).get('zh', [food['id']])[0]}  |  {food['id']}")

    def current_image_name(self):
        return self.images[self.current_index].name if self.images else ""

    def refresh_image(self):
        if not self.images:
            self.status_var.set("没有发现 JPG、PNG、WEBP 或 BMP 源图片")
            self.image_label.configure(text="所有源图片都已完成映射")
            return
        path = self.images[self.current_index]
        assigned = self.mapping.get(path.name)
        self.status_var.set(f"{self.current_index + 1} / {len(self.images)}    {path.name}" + (f"    已映射：{assigned}" if assigned else "    未映射"))
        try:
            from PIL import Image, ImageTk
            with Image.open(path) as image:
                image.thumbnail((410, 560), Image.Resampling.LANCZOS)
                self.photo = ImageTk.PhotoImage(image.copy())
            self.image_label.configure(image=self.photo, text="")
        except ImportError:
            self.image_label.configure(text="需要在 uv 环境安装 Pillow 才能预览图片", image="")
        except Exception as error:
            self.image_label.configure(text=f"图片无法读取：{error}", image="")
        self.selection_var.set("未选择食物")
        self.refresh_foods()

    def save_and_next(self):
        selected = self.food_list.curselection()
        if not selected:
            messagebox.showwarning("需要选择", "请先从列表中选择对应的食材、食物或菜肴。")
            return
        if not self.images:
            return
        food = self.filtered_foods[selected[0]]
        image_name = self.current_image_name()
        previous = next((name for name, value in self.mapping.items() if value == food["id"] and name != image_name), None)
        if previous:
            messagebox.showwarning("已存在映射", f"该食品已经映射了源图片：{previous}")
            return
        self.mapping[image_name] = food["id"]
        self.write_mapping()
        self.images.pop(self.current_index)
        if self.images:
            self.current_index %= len(self.images)
        self.refresh_image()

    def skip_image(self):
        self.next_image()

    def next_image(self):
        if not self.images:
            return
        self.current_index = (self.current_index + 1) % len(self.images)
        self.refresh_image()

    def write_mapping(self):
        temporary = MAPPING_FILE.with_suffix(".json.tmp")
        temporary.write_text(json.dumps(dict(sorted(self.mapping.items())), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        temporary.replace(MAPPING_FILE)


def main():
    root = tk.Tk()
    ImageMapper(root)
    root.mainloop()


if __name__ == "__main__":
    main()
