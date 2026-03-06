import os
import shutil
import re

base_dir = r"c:\Users\ppava\IdeaProjects\Robbery\src\main\java"
old_backups_dir = os.path.join(base_dir, "oldBackups")

if not os.path.exists(old_backups_dir):
    print("No oldBackups dir")
    exit()

moves = {
    "DoorArea.java": "robbery.doors",
    "DoorVisibilityListener.java": "robbery.doors",
    "EntityHitNotifier.java": "robbery.mechanics",
    "ItemChatListener.java": "robbery.chat",
    "ItemManager.java": "robbery.items",
    "Migrate.java": "robbery.core",
    "PickupPreventionListener.java": "robbery.mechanics",
    "ReloadShop.java": "robbery.shop",
    "Shop.java": "robbery.shop",
    "ShopBuy.java": "robbery.shop",
    "ShopManager.java": "robbery.shop",
}

for file_name, new_pkg in moves.items():
    file_path = os.path.join(old_backups_dir, file_name)
    if not os.path.exists(file_path):
        continue
    
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()
    
    # Check for conflict
    new_dir = os.path.join(base_dir, *new_pkg.split("."))
    os.makedirs(new_dir, exist_ok=True)
    
    new_file_name = file_name
    class_name = file_name[:-5]
    if os.path.exists(os.path.join(new_dir, file_name)):
        new_file_name = "Old" + file_name
        new_class_name = "Old" + class_name
        content = content.replace(f"class {class_name}", f"class {new_class_name}")
        content = content.replace(f"public {class_name}", f"public {new_class_name}")
    
    def replace_pkg(m):
        prefix = m.group(1) or ""
        return f"{prefix}package {new_pkg};"

    content = re.sub(r'(/\*\s*)?package\s+[\w.]+;', replace_pkg, content)
    
    new_path = os.path.join(new_dir, new_file_name)
    
    with open(new_path, "w", encoding="utf-8") as f:
        f.write(content)
    
    os.remove(file_path)

if not os.listdir(old_backups_dir):
    os.rmdir(old_backups_dir)
print("Done processing oldBackups")
