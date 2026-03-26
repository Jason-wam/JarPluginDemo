@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

:: 检查是否传入了参数
if "%~1"=="" (
    echo 请输入Jar文件!
    echo 当前工作目录: %cd%
    echo 参数数量: %#
    exit /b 1
)

:: 验证输入文件是否存在且为合法路径
set "input_file=%~1"
:: 使用双引号包围路径来处理空格
if not exist "!input_file!" (
    echo 错误: 文件不存在: !input_file!
    exit /b 1
)

:: 验证输入文件扩展名
set "ext=!input_file:~-4!"
if /i not "!ext!"==".jar" (
    echo 警告: 输入文件不是.jar格式: !input_file!
)

:: 验证必需工具是否存在
if not exist "%~dp0d8.jar" (
    echo 错误: 找不到 d8.jar 工具，请确保该文件存在于脚本同目录下
    exit /b 1
)

if not exist "%~dp0zipalign.exe" (
    echo 错误: 找不到 zipalign.exe 工具，请确保该文件存在于脚本同目录下
    exit /b 1
)

:: 构建输出文件路径
set "output=%~dp1%~n1_Dex%~x1"
set "output_4K=%~dp1%~n1_Dex_4K_Aligned%~x1"

:: 安全地删除已存在的输出文件
if exist "%output%" (
    del "%output%" 2>nul
    if exist "%output%" (
        echo 警告: 无法删除现有输出文件: %output%
    )
)
if exist "%output_4K%" (
    del "%output_4K%" 2>nul
    if exist "%output_4K%" (
        echo 警告: 无法删除现有输出文件: %output_4K%
    )
)

:: 确保在正确的目录下执行
pushd "%~dp0"

echo.
echo 正在转换Jar文件为Dex...
:: 使用双引号包围所有路径来处理空格
java -jar "%~dp0d8.jar" --output "%output%" "%~1"
if errorlevel 1 (
    popd
    echo D8 转换失败
    echo 可能的原因:
    echo - Java环境未正确安装或配置
    echo - d8.jar文件损坏
    echo - 输入文件格式不支持
    exit /b 1
)

echo.
echo 正在进行4K对齐...
:: 使用双引号包围所有路径来处理空格
"%~dp0zipalign.exe" -v 4 "%output%" "%output_4K%"
if errorlevel 1 (
    popd
    echo Zipalign 4K对齐失败
    echo 可能的原因:
    echo - zipalign.exe文件损坏
    echo - 输入的DEX文件格式不正确
    exit /b 1
)

popd

echo.
echo 转换完成！输出文件: %output_4K%
