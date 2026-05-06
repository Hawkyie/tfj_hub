import { SafeAreaView, StyleSheet, Text } from 'react-native';

export default function OperationsScreen() {
  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.title}>Servers</Text>
      <Text style={styles.subtitle}>Upcoming TFJ Servers will appear here.</Text>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#111827',
    padding: 20,
  },
  title: {
    color: 'white',
    fontSize: 32,
    fontWeight: 'bold',
    marginTop: 20,
  },
  subtitle: {
    color: '#9CA3AF',
    fontSize: 16,
    marginTop: 8,
  },
});