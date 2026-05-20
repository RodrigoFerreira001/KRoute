import re
from pathlib import Path


def define_env(env):
    toml_path = Path(env.project_dir) / "gradle" / "libs.versions.toml"
    text = toml_path.read_text()

    match = re.search(r'kroute\s*=\s*"([^"]+)"', text)
    if match:
        env.variables["version"] = match.group(1)