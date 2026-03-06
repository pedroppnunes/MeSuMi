import os
import shutil
import re

base_dir = r"c:\Users\ppava\IdeaProjects\Robbery\src\main\java"

class_moves = {
    # commands to new packages
    "robbery.commands.AddItem": "robbery.items.AddItem",
    "robbery.commands.RemoveItem": "robbery.items.RemoveItem",
    "robbery.commands.Baltop": "robbery.economy.Baltop",
    "robbery.commands.Sell": "robbery.economy.Sell",
    "robbery.commands.Busted": "robbery.mechanics.Busted",
    "robbery.commands.BuyBackpack": "robbery.backpacks.BuyBackpack",
    "robbery.commands.PvCommand": "robbery.backpacks.PvCommand",
    "robbery.commands.BuyKey": "robbery.keys.BuyKey",
    "robbery.commands.Rcrate": "robbery.keys.Rcrate",
    "robbery.commands.BuyTool": "robbery.tool.BuyTool",
    "robbery.commands.ChatColorCommand": "robbery.chat.ChatColorCommand",
    "robbery.commands.Claim": "robbery.claim.Claim",
    "robbery.commands.HelpCommand": "robbery.core.HelpCommand",
    "robbery.commands.Load": "robbery.core.Load",
    "robbery.commands.LoadBackup": "robbery.core.LoadBackup",
    "robbery.commands.MigrateBackup": "robbery.core.MigrateBackup",
    "robbery.commands.RobberyReload": "robbery.core.RobberyReload",
    "robbery.commands.HidePlayers": "robbery.mechanics.HidePlayers",
    "robbery.commands.NightVision": "robbery.mechanics.NightVision",
    "robbery.commands.ToggleDoubleJump": "robbery.mechanics.ToggleDoubleJump",
    "robbery.commands.Lobby": "robbery.teleport.Lobby",
    "robbery.commands.Mall": "robbery.teleport.Mall",
    "robbery.commands.SpawnCommand": "robbery.teleport.SpawnCommand",
    "robbery.commands.StoreTeleport": "robbery.teleport.StoreTeleport",
    "robbery.commands.MuteCommand": "robbery.mutes.MuteCommand",
    "robbery.commands.MuteInfoCommand": "robbery.mutes.MuteInfoCommand",
    "robbery.commands.UnmuteCommand": "robbery.mutes.UnmuteCommand",
    "robbery.commands.Outpost": "robbery.outpost.Outpost",
    "robbery.commands.Prestige": "robbery.prestige.Prestige",
    "robbery.commands.RankUp": "robbery.ranks.RankUp",
    "robbery.commands.RankUpdate": "robbery.ranks.RankUpdate",
    "robbery.commands.SkillpointBuy": "robbery.skillpoints.SkillpointBuy",
    "robbery.commands.StopBoosterCommand": "robbery.booster.StopBoosterCommand",
    "robbery.commands.UseBooster": "robbery.booster.UseBooster",
    "robbery.commands.WarnCommand": "robbery.warnings.WarnCommand",
    "robbery.commands.WarningsCommand": "robbery.warnings.WarningsCommand",
    "robbery.commands.WeeklyLeaderboardCommand": "robbery.leaderboard.WeeklyLeaderboardCommand",
    
    # events to new packages
    "robbery.events.ArmorStandInteractionListener": "robbery.mechanics.ArmorStandInteractionListener",
    "robbery.events.BlockCraftListener": "robbery.mechanics.BlockCraftListener",
    "robbery.events.DoubleJumpListener": "robbery.mechanics.DoubleJumpListener",
    "robbery.events.HideoutListener": "robbery.mechanics.HideoutListener",
    "robbery.events.InventoryLockListener": "robbery.mechanics.InventoryLockListener",
    "robbery.events.InventoryManager": "robbery.mechanics.InventoryManager",
    "robbery.events.PickingTask": "robbery.mechanics.PickingTask",
    "robbery.events.PickupPreventionListener": "robbery.mechanics.PickupPreventionListener",
    "robbery.events.AutoReloadTask": "robbery.core.AutoReloadTask",
    "robbery.events.RewardHolder": "robbery.core.RewardHolder",
    "robbery.events.ChatItemReplacer": "robbery.chat.ChatItemReplacer",
    "robbery.events.ClaimGuiListener": "robbery.claim.ClaimGuiListener",
    "robbery.events.HourlyLeaderboard": "robbery.leaderboard.HourlyLeaderboard",
    "robbery.events.WeeklyLeaderboardTask": "robbery.leaderboard.WeeklyLeaderboardTask",
    "robbery.events.VoteListener": "robbery.votes.VoteListener",
    
    # root
    "robbery.Robbery": "robbery.core.Robbery",
    "robbery.RobberyPlaceholderExpansion": "robbery.core.RobberyPlaceholderExpansion",
}

java_files = []
for root, dirs, files in os.walk(base_dir):
    for f in files:
        if f.endswith(".java"):
            java_files.append(os.path.join(root, f))

file_contents = {}
for f in java_files:
    try:
        with open(f, 'r', encoding='utf-8') as file:
            file_contents[f] = file.read()
    except Exception as e:
        print(f"Error reading {f}: {e}")

# Build regexes for imports
import_regexes = []
for old_fqcn, new_fqcn in class_moves.items():
    import_regexes.append((re.compile(r'import\s+' + old_fqcn.replace('.', r'\.') + r'\s*;'), f"import {new_fqcn};"))

wildcard_imports = {
    "robbery.commands.*": [new_fqcn for old, new_fqcn in class_moves.items() if old.startswith("robbery.commands.")],
    "robbery.events.*": [new_fqcn for old, new_fqcn in class_moves.items() if old.startswith("robbery.events.")]
}

def replace_wildcards(content):
    for wc, new_classes in wildcard_imports.items():
        if f"import {wc};" in content:
            replacement = "\n".join([f"import {cls};" for cls in new_classes])
            content = content.replace(f"import {wc};", replacement)
    return content

# Modify contents
for f, content in file_contents.items():
    new_content = replace_wildcards(content)
    for regex, replacement in import_regexes:
        new_content = regex.sub(replacement, new_content)
    
    # Check if this file itself is being moved to update its package declaration
    rel_path = os.path.relpath(f, base_dir)
    fqcn = rel_path.replace(os.sep, '.')[:-5]
    if fqcn in class_moves:
        new_fqcn = class_moves[fqcn]
        new_pkg = new_fqcn.rsplit('.', 1)[0]
        # Replace package statement
        new_content = re.sub(r'package\s+[\w.]+;', f"package {new_pkg};", new_content)
    file_contents[f] = new_content

# Write back
for f, content in file_contents.items():
    with open(f, 'w', encoding='utf-8') as file:
        file.write(content)

# Move files
for f in java_files:
    rel_path = os.path.relpath(f, base_dir)
    fqcn = rel_path.replace(os.sep, '.')[:-5]
    if fqcn in class_moves:
        new_fqcn = class_moves[fqcn]
        new_rel_path = new_fqcn.replace('.', os.sep) + ".java"
        new_path = os.path.join(base_dir, new_rel_path)
        os.makedirs(os.path.dirname(new_path), exist_ok=True)
        shutil.move(f, new_path)

# Empty dirs cleanup
for root, dirs, files in os.walk(base_dir, topdown=False):
    t = os.listdir(root)
    if not t:
        try:
            os.rmdir(root)
        except OSError:
            pass
print("Refactor complete.")
