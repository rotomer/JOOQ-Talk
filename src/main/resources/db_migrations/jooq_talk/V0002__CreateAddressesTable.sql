
CREATE TABLE [dbo].[Addresses](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[customer_id] [bigint] NOT NULL,
	[address_line_1] [nvarchar](200) NOT NULL,
	[address_line_2] [nvarchar](200) NULL,
	[city] [nvarchar](100) NOT NULL,
	[state] [nvarchar](100) NULL,
	[country] [nvarchar](100) NOT NULL,
	[zip] [nvarchar](50) NULL,
 CONSTRAINT [PK_Addresses] PRIMARY KEY CLUSTERED
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
