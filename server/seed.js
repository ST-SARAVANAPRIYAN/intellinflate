require('dotenv').config();
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

const User = require('./models/User');

const MONGODB_URI = process.env.MONGODB_URI || '';
const MONGODB_DB = process.env.MONGODB_DB || 'intellinflate_db';

function normalizePlate(value = '') {
  return value.toUpperCase().replace(/\s/g, '');
}

const seedUsers = [
  {
    username: 'saravana',
    email: 'saravana',
    password: '1234567890',
    numberPlate: 'RJ1LCV0002',
    vehicleModel: 'Hyundai i20',
    phone: '9000000001',
  },
  {
    username: 'demo_user_2',
    email: 'demo2@intellinflate.local',
    password: 'n@123',
    numberPlate: 'TN10CD5678',
    vehicleModel: 'Honda City',
    phone: '9000000002',
  },
  {
    username: 'demo_user_3',
    email: 'demo3@intellinflate.local',
    password: 'Demo@123',
    numberPlate: 'KA05EF4321',
    vehicleModel: 'Tata Nexon',
    phone: '9000000003',
  },
];

async function seed() {
  if (!MONGODB_URI || MONGODB_URI.includes('<username>')) {
    console.error('MONGODB_URI is missing or invalid. Set it in .env before running seed.');
    process.exit(1);
  }

  await mongoose.connect(MONGODB_URI, {
    dbName: MONGODB_DB,
    family: 4,
    serverSelectionTimeoutMS: 30000,
    connectTimeoutMS: 30000,
  });

  let created = 0;
  let updated = 0;

  for (const user of seedUsers) {
    const hashedPassword = await bcrypt.hash(user.password, 10);
    const payload = {
      username: user.username,
      email: user.email.toLowerCase(),
      password: hashedPassword,
      numberPlate: normalizePlate(user.numberPlate),
      vehicleModel: user.vehicleModel,
      phone: user.phone,
      updatedAt: new Date(),
    };

    const existing = await User.findOne({
      $or: [{ email: payload.email }, { numberPlate: payload.numberPlate }],
    });

    if (existing) {
      existing.username = payload.username;
      existing.email = payload.email;
      existing.password = payload.password;
      existing.numberPlate = payload.numberPlate;
      existing.vehicleModel = payload.vehicleModel;
      existing.phone = payload.phone;
      existing.updatedAt = payload.updatedAt;
      await existing.save();
      updated += 1;
      continue;
    }

    await User.create(payload);
    created += 1;
  }

  console.log(`Seeding complete. created=${created}, updated=${updated}, total=${seedUsers.length}`);
}

seed()
  .catch((err) => {
    console.error('Seed failed:', err.message);
    process.exitCode = 1;
  })
  .finally(async () => {
    try {
      await mongoose.disconnect();
    } catch (err) {
      // ignore disconnect errors
    }
  });
