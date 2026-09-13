# 校园咸鱼 - 本地数据库一键导入脚本
# 用法（在 PowerShell 中）：
#   cd d:\campus-xianyu
#   .\import-db.ps1
# 如果 MySQL 有密码：
#   $env:MYSQL_PWD="你的密码"; .\import-db.ps1

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$MysqlExe = "D:\MySQL\bin\mysql.exe"

if (-not (Test-Path $MysqlExe)) {
    $MysqlExe = (Get-Command mysql -ErrorAction SilentlyContinue).Source
}
if (-not $MysqlExe) {
    Write-Host "未找到 mysql 命令，请确认 MySQL 已安装并在 PATH 中。" -ForegroundColor Red
    exit 1
}

$SchemaFile = Join-Path $ProjectRoot "sql\01_schema.sql"
$SeedFile = Join-Path $ProjectRoot "sql\02_seed_data.sql"

if (-not (Test-Path $SchemaFile)) {
    Write-Host "找不到 $SchemaFile" -ForegroundColor Red
    exit 1
}

Write-Host "正在连接 MySQL 并创建数据库 campus_xianyu ..." -ForegroundColor Cyan
& $MysqlExe -uroot -e "CREATE DATABASE IF NOT EXISTS campus_xianyu DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;"

Write-Host "正在导入表结构 (01_schema.sql) ..." -ForegroundColor Cyan
Get-Content $SchemaFile -Raw -Encoding UTF8 | & $MysqlExe -uroot campus_xianyu

Write-Host "正在导入基础数据 (02_seed_data.sql) ..." -ForegroundColor Cyan
Get-Content $SeedFile -Raw -Encoding UTF8 | & $MysqlExe -uroot campus_xianyu

Write-Host ""
Write-Host "导入完成！" -ForegroundColor Green
Write-Host "数据库名: campus_xianyu"
Write-Host "管理员账号: admin / password"
Write-Host ""
Write-Host "可用以下命令验证：" -ForegroundColor Yellow
Write-Host "  mysql -uroot -e `"USE campus_xianyu; SHOW TABLES;`""
