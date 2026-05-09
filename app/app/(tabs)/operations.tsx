import {
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
} from 'react-native';

import { router } from 'expo-router';
import OperationCard from '../../components/OperationCard';
import { useEffect, useState } from 'react';
import { api } from '../../services/api';

export default function OperationsScreen() {
  const [operations, setOperations] = useState<any[]>([]);

useEffect(() => {
  const fetchOperations = async () => {
    try {
      const response = await api.get('/operations');
      console.log(response.data);
setOperations(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error('Failed to fetch operations', error);
    }
  };

  fetchOperations();
}, []);
  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.title}>Operations</Text>

      <Text style={styles.subtitle}>
        Upcoming Task Force Jackal operations
      </Text>

      <ScrollView style={styles.content}>
  {operations.map((operation) => (
  <OperationCard
    id={operation.id}
    key={operation.id}
    title={operation.title}
    date={operation.date}
    description={operation.description}
    type={operation.type}
    onPress={() =>
      router.push({
        pathname: '/operation-details',
        params: {
          id: operation.id.toString(),
        },
      })
    }
  />
))}
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
    marginBottom: 20,
  },

  content: {
    flex: 1,
    paddingHorizontal: 20,
  },
});