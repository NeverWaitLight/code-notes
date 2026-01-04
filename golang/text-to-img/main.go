package main

import (
	"fmt"
	"image"
	"image/color"
	"image/draw"
	"image/png"
	"math/rand"
	"os"
	"time"

	"golang.org/x/image/font"
	"golang.org/x/image/font/opentype"
	"golang.org/x/image/math/fixed"
)

func main() {
	// 创建300x300的RGBA图像
	img := image.NewRGBA(image.Rect(0, 0, 300, 300))

	// 生成随机彩色背景
	rng := rand.New(rand.NewSource(time.Now().UnixNano()))
	bgColor := color.RGBA{
		R: uint8(rng.Intn(256)),
		G: uint8(rng.Intn(256)),
		B: uint8(rng.Intn(256)),
		A: 255,
	}

	// 填充背景色
	draw.Draw(img, img.Bounds(), &image.Uniform{bgColor}, image.Point{}, draw.Src)

	// 直接加载思源宋体OTF文件
	fontPath := `C:\Users\admin\Downloads\SiYuanSongTiRegular\SourceHanSerifCN-Bold-2.otf`
	fontBytes, err := os.ReadFile(fontPath)
	if err != nil {
		fmt.Printf("读取OTF字体文件失败: %v\n", err)
		return
	}

	fontData, err := opentype.Parse(fontBytes)
	if err != nil {
		fmt.Printf("解析OTF字体文件失败: %v\n", err)
		return
	}

	// 创建字体面
	const fontSize = 48
	const dpi = 72
	face, err := opentype.NewFace(fontData, &opentype.FaceOptions{
		Size:    fontSize,
		DPI:     dpi,
		Hinting: font.HintingFull,
	})
	if err != nil {
		fmt.Printf("创建字体面失败: %v\n", err)
		return
	}
	defer face.Close()

	// 要显示的文字
	text := "思源宋体"

	// 计算文字居中位置
	bounds, _ := font.BoundString(face, text)
	textWidth := bounds.Max.X - bounds.Min.X
	textHeight := bounds.Max.Y - bounds.Min.Y

	x := (300 - int(textWidth>>6)) / 2
	y := (300 + int(textHeight>>6)) / 2

	// 绘制文字
	drawer := &font.Drawer{
		Dst:  img,
		Src:  image.NewUniform(color.RGBA{255, 255, 255, 255}), // 白色文字
		Face: face,
		Dot:  fixed.Point26_6{X: fixed.Int26_6(x << 6), Y: fixed.Int26_6(y << 6)},
	}
	drawer.DrawString(text)

	// 生成带时间戳的文件名
	timestamp := time.Now().Format("20060102_150405")
	filename := fmt.Sprintf("output_%s.png", timestamp)

	// 保存图像
	file, err := os.Create(filename)
	if err != nil {
		fmt.Printf("创建文件失败: %v\n", err)
		return
	}
	defer file.Close()

	err = png.Encode(file, img)
	if err != nil {
		fmt.Printf("保存图像失败: %v\n", err)
		return
	}

	fmt.Printf("图片已保存为: %s\n", filename)
}
