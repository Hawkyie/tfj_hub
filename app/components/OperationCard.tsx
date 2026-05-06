import { useState } from 'react';

import {
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';

type OperationCardProps = {
  title: string;
  date: string;
  description: string;
  type: string;
};

export default function OperationCard({
  title,
  date,
  description,
  type,
}: OperationCardProps) {
  const [attending, setAttending] = useState(false);

  return (
    <View style={styles.card}>
      <Text style={styles.badge}>{type}</Text>

      <Text style={styles.opTitle}>{title}</Text>

      <Text style={styles.opDate}>{date}</Text>

      <Text style={styles.opDescription}>{description}</Text>

      <Text style={styles.statusText}>
        Status: {attending ? 'Attending' : 'No response'}
      </Text>

      <TouchableOpacity
        style={[
          styles.attendButton,
          attending && styles.attendButtonActive,
        ]}
        onPress={() => setAttending(!attending)}
      >
        <Text style={styles.buttonText}>
          {attending ? 'Cancel Attendance' : 'Mark Attending'}
        </Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: '#1F2937',
    borderRadius: 16,
    padding: 20,
    marginBottom: 16,
  },

  badge: {
    color: '#93C5FD',
    fontSize: 12,
    fontWeight: 'bold',
    marginBottom: 8,
    textTransform: 'uppercase',
  },

  opTitle: {
    color: 'white',
    fontSize: 22,
    fontWeight: 'bold',
    marginBottom: 8,
  },

  opDate: {
    color: '#60A5FA',
    fontSize: 14,
    marginBottom: 12,
  },

  opDescription: {
    color: '#D1D5DB',
    fontSize: 15,
    lineHeight: 22,
    marginBottom: 16,
  },

  statusText: {
    color: '#D1D5DB',
    fontSize: 14,
    marginBottom: 14,
  },

  attendButton: {
    backgroundColor: '#2563EB',
    paddingVertical: 12,
    borderRadius: 10,
    alignItems: 'center',
  },

  attendButtonActive: {
    backgroundColor: '#166534',
  },

  buttonText: {
    color: 'white',
    fontWeight: 'bold',
  },
});