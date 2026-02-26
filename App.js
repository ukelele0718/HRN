import React from "react";
import { View, Pressable, Text, StyleSheet, NativeModules, ScrollView } from "react-native";

const { NativeLauncher } = NativeModules;

export default function App() {
  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Text style={styles.title}>HAND PARTICLE APP</Text>
      
      <Pressable
        style={styles.button}
        onPress={() => NativeLauncher.openNativeMain()}
      >
        <Text style={styles.buttonText}>🖐️ MỞ HAND PARTICLE</Text>
      </Pressable>

      <Pressable
        style={[styles.button, styles.buttonGreen]}
        onPress={() => NativeLauncher.openNumberInput()}
      >
        <Text style={styles.buttonText}>🔢 NHẬP SỐ BẰNG CỬ CHỈ</Text>
      </Pressable>

      <Pressable
        style={[styles.button, styles.buttonBlue]}
        onPress={() => NativeLauncher.openTextInput()}
      >
        <Text style={styles.buttonText}>🔤 NHẬP CHỮ BẰNG CỬ CHỈ</Text>
      </Pressable>

      <View style={styles.infoBox}>
        <Text style={styles.infoTitle}>Hướng dẫn điều khiển:</Text>
        <Text style={styles.infoText}>• Chỉ tay để di chuyển ↑↓←→</Text>
        <Text style={styles.infoText}>• Hướng cánh tay (khuỷu → cổ → ngón trỏ) = hướng di chuyển</Text>
        <Text style={styles.infoText}>• Giơ 2 tay tạo hình chữ O để chọn</Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flexGrow: 1,
    backgroundColor: "#1a1a2e",
    alignItems: "center",
    justifyContent: "center",
    padding: 20,
  },
  title: {
    color: "#ffffff",
    fontSize: 24,
    fontWeight: "bold",
    marginBottom: 40,
  },
  button: {
    backgroundColor: "#e94560",
    paddingVertical: 16,
    paddingHorizontal: 28,
    borderRadius: 12,
    marginVertical: 10,
    width: "100%",
    alignItems: "center",
  },
  buttonGreen: {
    backgroundColor: "#00a86b",
  },
  buttonBlue: {
    backgroundColor: "#0077b6",
  },
  buttonText: {
    color: "white",
    fontWeight: "700",
    fontSize: 18,
  },
  infoBox: {
    marginTop: 40,
    padding: 20,
    backgroundColor: "#16213e",
    borderRadius: 12,
    width: "100%",
  },
  infoTitle: {
    color: "#ffcc00",
    fontSize: 16,
    fontWeight: "bold",
    marginBottom: 10,
  },
  infoText: {
    color: "#aaaaaa",
    fontSize: 14,
    marginVertical: 4,
  },
});
