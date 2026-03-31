// This is a basic Flutter widget test.
//
// To perform an interaction with a widget in your test, use the WidgetTester
// utility in the flutter_test package. For example, you can send tap and scroll
// gestures. You can also use WidgetTester to find child widgets in the widget
// tree, read text, and verify that the values of widget properties are correct.

import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';

import 'package:intellinflate/main.dart';
import 'package:intellinflate/viewmodels/intellinflate_viewmodel.dart';

void main() {
  testWidgets('App boots to auth flow', (WidgetTester tester) async {
    await tester.pumpWidget(
      ChangeNotifierProvider(
        create: (_) => IntelliInflateViewModel(),
        child: const IntelliInflateApp(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('IntelliInflate'), findsWidgets);
  });
}
