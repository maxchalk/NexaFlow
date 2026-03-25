import psycopg2
import pandas as pd
from reportlab.lib.pagesizes import letter
from reportlab.platypus import SimpleDocTemplate, Paragraph, Table, TableStyle, Spacer
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.lib import colors
from datetime import datetime
import schedule
import time
import os


def get_connection():
    db_url = os.getenv('DATABASE_URL_ORDERS', 'jdbc:postgresql://localhost:5433/orders_db')
    # Convert JDBC URL to psycopg2 format
    pg_url = db_url.replace('jdbc:postgresql://', 'postgresql://')
    # Add credentials
    if '?' in pg_url:
        pg_url = pg_url.split('?')[0]
    host_port_db = pg_url.replace('postgresql://', '')
    parts = host_port_db.split('/')
    host_port = parts[0].split(':')
    host = host_port[0]
    port = host_port[1] if len(host_port) > 1 else '5433'
    dbname = parts[1] if len(parts) > 1 else 'orders_db'

    return psycopg2.connect(
        host=host,
        port=port,
        dbname=dbname,
        user=os.getenv('DB_USER', 'postgres'),
        password=os.getenv('DB_PASSWORD', 'postgres123')
    )


def generate_daily_report():
    print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] Generating daily report...")

    try:
        conn = get_connection()
    except Exception as e:
        print(f"Database connection failed: {e}")
        return

    try:
        # Query 1: Orders by status today
        orders_df = pd.read_sql("""
            SELECT status, COUNT(*) as count, COALESCE(SUM(total_amount), 0) as revenue
            FROM orders
            WHERE created_at >= NOW() - INTERVAL '24 hours'
            GROUP BY status
            ORDER BY count DESC
        """, conn)

        # Query 2: Top products ordered this week
        top_products_df = pd.read_sql("""
            SELECT oi.product_name,
                   SUM(oi.quantity) as total_ordered,
                   SUM(oi.subtotal) as total_revenue
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            WHERE o.created_at >= NOW() - INTERVAL '7 days'
              AND o.status != 'CANCELLED'
            GROUP BY oi.product_name
            ORDER BY total_ordered DESC
            LIMIT 10
        """, conn)

        # Query 3: Daily order trend last 7 days
        trend_df = pd.read_sql("""
            SELECT DATE(created_at) as date,
                   COUNT(*) as orders,
                   COALESCE(SUM(total_amount), 0) as revenue
            FROM orders
            WHERE created_at >= NOW() - INTERVAL '7 days'
            GROUP BY DATE(created_at)
            ORDER BY date
        """, conn)

    except Exception as e:
        print(f"Query failed: {e}")
        conn.close()
        return

    conn.close()

    # Generate PDF
    os.makedirs('reports', exist_ok=True)
    filename = f"reports/daily_report_{datetime.now().strftime('%Y%m%d_%H%M')}.pdf"
    doc = SimpleDocTemplate(filename, pagesize=letter,
                            leftMargin=50, rightMargin=50, topMargin=50, bottomMargin=50)
    styles = getSampleStyleSheet()
    story = []

    # Title
    story.append(Paragraph("NexaFlow Daily Analytics Report", styles['Title']))
    story.append(Paragraph(
        f"Generated: {datetime.now().strftime('%B %d, %Y at %H:%M')}",
        styles['Normal']
    ))
    story.append(Spacer(1, 20))

    # Orders by status
    story.append(Paragraph("Orders by Status (Last 24 Hours)", styles['Heading2']))
    if not orders_df.empty:
        data = [['Status', 'Count', 'Revenue']] + [
            [row['status'], str(int(row['count'])), f"${float(row['revenue']):.2f}"]
            for _, row in orders_df.iterrows()
        ]
        t = Table(data, colWidths=[200, 100, 150])
        t.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#16213e')),
            ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
            ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
            ('GRID', (0, 0), (-1, -1), 0.5, colors.grey),
            ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.HexColor('#f8f9fa'), colors.white]),
            ('ALIGN', (1, 0), (-1, -1), 'CENTER'),
        ]))
        story.append(t)
    else:
        story.append(Paragraph("No orders in the last 24 hours.", styles['Normal']))

    story.append(Spacer(1, 20))

    # Top products
    story.append(Paragraph("Top 10 Products This Week", styles['Heading2']))
    if not top_products_df.empty:
        data = [['Product', 'Units Sold', 'Revenue']] + [
            [row['product_name'], str(int(row['total_ordered'])), f"${float(row['total_revenue']):.2f}"]
            for _, row in top_products_df.iterrows()
        ]
        t = Table(data, colWidths=[250, 100, 100])
        t.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#16213e')),
            ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
            ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
            ('GRID', (0, 0), (-1, -1), 0.5, colors.grey),
            ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.HexColor('#f8f9fa'), colors.white]),
            ('ALIGN', (1, 0), (-1, -1), 'CENTER'),
        ]))
        story.append(t)
    else:
        story.append(Paragraph("No product data available for this week.", styles['Normal']))

    story.append(Spacer(1, 20))

    # 7-day trend
    story.append(Paragraph("7-Day Order Trend", styles['Heading2']))
    if not trend_df.empty:
        data = [['Date', 'Orders', 'Revenue']] + [
            [str(row['date']), str(int(row['orders'])), f"${float(row['revenue']):.2f}"]
            for _, row in trend_df.iterrows()
        ]
        t = Table(data, colWidths=[150, 100, 150])
        t.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#16213e')),
            ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
            ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
            ('GRID', (0, 0), (-1, -1), 0.5, colors.grey),
            ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.HexColor('#f8f9fa'), colors.white]),
            ('ALIGN', (1, 0), (-1, -1), 'CENTER'),
        ]))
        story.append(t)

    doc.build(story)
    print(f"Report generated: {filename}")


if __name__ == '__main__':
    # Run immediately on start
    generate_daily_report()

    # Schedule nightly at midnight
    schedule.every().day.at("00:00").do(generate_daily_report)

    print("Scheduler running. Reports will generate nightly at midnight.")
    while True:
        schedule.run_pending()
        time.sleep(60)
