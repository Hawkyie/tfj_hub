import { Router } from 'express';

const router = Router();

router.get('/', (req, res) => {
  res.json([
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
  ]);
});

export default router;