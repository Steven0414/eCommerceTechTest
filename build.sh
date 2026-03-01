#!/bin/bash

set -e

echo "🚀 Building Event-Driven E-Commerce System..."
echo "================================================"

# Build order-service
echo ""
echo "📦 Building order-service..."
cd order-service
mvn clean package -DskipTests
cd ..

# Build payment-service
echo ""
echo "💳 Building payment-service..."
cd payment-service
mvn clean package -DskipTests
cd ..

# Build notification-service
echo ""
echo "📧 Building notification-service..."
cd notification-service
mvn clean package -DskipTests
cd ..

echo ""
echo "✅ All services built successfully!"
echo ""
echo "To start the system, run:"
echo "  docker-compose up --build"
