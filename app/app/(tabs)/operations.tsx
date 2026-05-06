import {
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
} from 'react-native';

import OperationCard from '../../components/OperationCard';
import { router } from 'expo-router';

const operations = [
  {
    id: 1,
    title: 'Operation Iron Spear',
    date: 'Saturday • 1900 BST',
    description:
      'NATO forces conduct a raid against insurgent weapon caches in the region.',
    type: 'Operation',
  },
  {
    id: 2,
    title: 'Operation Silent Dagger',
    date: 'Sunday • 1800 BST',
    description:
      'TFJ reconnaissance teams gather intelligence ahead of a major assault.',
    type: 'Operation',
  },
];

export default function OperationsScreen() {
  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.title}>Operations</Text>

      <Text style={styles.subtitle}>
        Upcoming Task Force Jackal operations
      </Text>

      <ScrollView style={styles.content}>
        {operations.map((operation) => (
          <OperationCard
            key={operation.id}
            title={operation.title}
            date={operation.date}
            description={operation.description}
            type={operation.type}
            onPress={() =>
              router.push({
                pathname: '/operation-details',
                params: {
                  title: operation.title,
                  date: operation.date,
                  description: operation.description,
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