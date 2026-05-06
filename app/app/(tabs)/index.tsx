import {
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
} from 'react-native';

export default function HomeScreen() {
  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.title}>TFJ Portal</Text>

      <Text style={styles.subtitle}>
        Task Force Jackal Member System
      </Text>

      <ScrollView style={styles.content}>
        <TouchableOpacity style={styles.card}>
          <Text style={styles.cardTitle}>Operations</Text>
          <Text style={styles.cardText}>
            View and respond to upcoming operations.
          </Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.card}>
          <Text style={styles.cardTitle}>Training</Text>
          <Text style={styles.cardText}>
            View training schedules and attendance.
          </Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.card}>
          <Text style={styles.cardTitle}>Servers</Text>
          <Text style={styles.cardText}>
            Manage and monitor TFJ servers.
          </Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.card}>
          <Text style={styles.cardTitle}>Personnel Record</Text>
          <Text style={styles.cardText}>
            View your TFJ member information.
          </Text>
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#111827',
    paddingTop: 20,
  },

  title: {
    color: 'white',
    fontSize: 32,
    fontWeight: 'bold',
    paddingHorizontal: 20,
    marginTop: 20,
  },

  subtitle: {
    color: '#9CA3AF',
    fontSize: 16,
    paddingHorizontal: 20,
    marginBottom: 30,
  },

  content: {
    flex: 1,
    paddingHorizontal: 20,
  },

  card: {
    backgroundColor: '#1F2937',
    borderRadius: 16,
    padding: 20,
    marginBottom: 16,
  },

  cardTitle: {
    color: 'white',
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 8,
  },

  cardText: {
    color: '#D1D5DB',
    fontSize: 14,
  },
});