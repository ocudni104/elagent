#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
monorepo_root="$(cd "${script_dir}/.." && pwd)"
git_root="$(git -C "${monorepo_root}" rev-parse --show-toplevel)"
git_dir="$(git -C "${monorepo_root}" rev-parse --git-dir)"
hook_path="${git_dir}/hooks/pre-commit"
monorepo_rel="$(realpath --relative-to="${git_root}" "${monorepo_root}")"

mkdir -p "$(dirname "${hook_path}")"

cat > "${hook_path}" <<EOF
#!/usr/bin/env bash
set -euo pipefail

git_root="\$(git rev-parse --show-toplevel)"
monorepo_dir="\${git_root}/${monorepo_rel}"
monorepo_rel="${monorepo_rel}"

if ! git diff --cached --name-only -- "\${monorepo_rel}" | grep -q .; then
  exit 0
fi

mapfile -t staged_services < <(
  git diff --cached --name-only -- "\${monorepo_rel}/services" \
    | awk -F/ -v prefix="\${monorepo_rel}/services/" '
        index(\$0, prefix) == 1 && NF >= 3 { print \$1 "/" \$2 "/" \$3 }
      ' \
    | sort -u
)

if [ "\${#staged_services[@]}" -eq 0 ]; then
  exit 0
fi

for service_dir in "\${staged_services[@]}"; do
  abs_service_dir="\${git_root}/\${service_dir}"
  if [ ! -f "\${abs_service_dir}/build.gradle.kts" ] || [ ! -x "\${abs_service_dir}/gradlew" ]; then
    continue
  fi

  echo "Running Spotless for \${service_dir}"
  (
    cd "\${abs_service_dir}"
    ./gradlew spotlessApply
  )

  git -C "\${git_root}" add -u -- "\${service_dir}"
done

if ! git -C "\${git_root}" diff --cached --quiet -- "\${monorepo_rel}/services"; then
  echo "Spotless applied for staged service changes and re-staged."
else
  echo "No Spotless formatting changes."
fi
EOF

chmod +x "${hook_path}"

echo "Installed pre-commit hook at ${hook_path}"
echo "The hook runs spotlessApply only for staged Gradle services under ${monorepo_rel}/services."
