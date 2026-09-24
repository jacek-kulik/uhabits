#!/bin/sh

repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
exec sh "$repo_dir/tools/install-git-hooks.sh"
