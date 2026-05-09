import { useState } from 'react';
import { router, useLocalSearchParams } from 'expo-router';
import { operations } from '../data/operations';
import { attendance } from '../data/attendance';

import {
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';

export default function OperationDetailsScreen() {
  const { id } = useLocalSearchParams();

    const operation = operations.find(
        (op) => op.id.toString() === id
    );
    const operationId = Number(id);

const [attending, setAttending] = useState(
  attendance[operationId] || false
);

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView style={styles.content}>
        <TouchableOpacity onPress={() => router.back()}>
        <Text style={styles.backButton}>← Back</Text>
        </TouchableOpacity>
        <Text style={styles.title}>{operation?.title}</Text>
        <Text style={styles.date}>{operation?.date}</Text>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Briefing</Text>
          <Text style={styles.description}>{operation?.description}</Text>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Required Mods</Text>
          <Text style={styles.description}>
            {operation?.mods}
          </Text>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Attendance</Text>
          <Text style={styles.description}>{operation?.attendingCount} Members Attending</Text>

          <TouchableOpacity
            style={[
              styles.attendButton,
              attending && styles.attendButtonActive,
            ]}
            onPress={() => {
  const newValue = !attending;

  setAttending(newValue);
  attendance[operationId] = newValue;
}}
          >
            <Text style={styles.buttonText}>
              {attending ? 'Cancel Attendance' : 'Mark Attending'}
            </Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#111827',
  },

  content: {
    padding: 20,
  },

  title: {
    color: 'white',
    fontSize: 30,
    fontWeight: 'bold',
    marginTop: 20,
    marginBottom: 10,
  },

  date: {
    color: '#60A5FA',
    fontSize: 16,
    marginBottom: 30,
  },

  section: {
    backgroundColor: '#1F2937',
    borderRadius: 16,
    padding: 20,
    marginBottom: 16,
  },

  sectionTitle: {
    color: 'white',
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 12,
  },

  description: {
    color: '#D1D5DB',
    fontSize: 15,
    lineHeight: 22,
  },

  attendButton: {
    backgroundColor: '#2563EB',
    paddingVertical: 14,
    borderRadius: 10,
    alignItems: 'center',
    marginTop: 20,
  },

  attendButtonActive: {
    backgroundColor: '#166534',
  },

  buttonText: {
    color: 'white',
    fontWeight: 'bold',
  },

  backButton: {
  color: '#60A5FA',
  fontSize: 16,
  marginTop: 20,
  marginBottom: 20,
},
});