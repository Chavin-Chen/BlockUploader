<?php
// 计算临时文件夹大小
function dir_size($dir)
{
    $size = 0;
    $dh = opendir($dir);
    $fname = "";
    while (false !== ($fname = @readdir($dh))) {
        if ($fname != '.' and $fname != '..') {
            $size += filesize("$dir$fname");
        }
    }
    closedir($dh);
    return $size;
}
// 合并分块文件夹
function dir_merge($dir, $filename)
{
    $files = array();
    $dh = opendir($dir);
    $fname = "";
    while (false !== ($fname = @readdir($dh))) {
        if ($fname != '.' and $fname != '..') {
            $files[] = intval($fname);
        }
    }
    closedir($dh);
    sort($files);
    $file = fopen($filename, 'w');
    foreach ($files as $fname) {
        $item_name = "$dir$fname";
        $item = fopen($item_name, 'r');
        fwrite($file, fread($item, filesize($item_name)));
        fclose($item);
    }
    fflush($file);
    fclose($file);
}
// 清理临时目录
function dir_clear($dir)
{
    $open = opendir($dir);
    while (($v = readdir($open)) !== false) {
        if ('.' == $v || '..' == $v) {
            continue;
        }
        unlink($dir . '/' . $v);
    }
    closedir($open);
    return rmdir($dir);
}
// 准备上传路径
if (!file_exists('_asset')) {
    mkdir('_asset');
}
if (!file_exists('_asset/tmp')) {
    mkdir('_asset/tmp');
}
// 请求头中包含文件信息
$headers = getallheaders();
$filename = $headers['Filename']; // 文件名
$total = $headers['Total']; // 文件总大小
$offset = $headers['Offset']; // 当前分块偏移

// 指定最终合并的临时文件
$final_tmp_name = "_asset/tmp/$filename";

// 为最终文件创建一个分块文件夹
$tmp_dir = "$final_tmp_name.dir/";
if (!file_exists($tmp_dir)) {
    mkdir($tmp_dir);
}
// 每个分块在临时文件夹中对应一个 .tmp 文件
$part_tmp = "$tmp_dir$offset.tmp";
$tmp_name = $_FILES['file']['tmp_name'];
if ($_FILES['file']['error'] === 0) {
    move_uploaded_file($tmp_name, $part_tmp);
}

// 添加文件锁
$final_file_lock = "$final_tmp_name.lock";
$fp = fopen($final_file_lock, 'w');
$code = 206;
if (flock($fp, LOCK_EX)) {
    // 将分块放入临时文件夹
    rename($part_tmp, "$tmp_dir/$offset");
    // 计算分块临时文件夹大小，若已完成全部分块上传，则合并
    $dir_size = intval(dir_size($tmp_dir));
    if ($dir_size >= intval($total)) {
        dir_merge($tmp_dir, $final_tmp_name);
        $code = 200;
    }
    flock($fp, LOCK_UN);
}
// 将合并的最终临时文件移动到目标文件，并清理相关临时文件
if ($code == 200) { 
    rename($final_tmp_name, "_asset/$filename");
    unlink($final_file_lock);
    dir_clear($tmp_dir);
}
// 206部分上传，200全部上传
http_response_code($code);
